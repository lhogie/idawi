package idawi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import idawi.knowledge_base.OperationDescriptor;
import idawi.knowledge_base.ServiceDescriptor;
import idawi.messaging.EOT;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.routing.MessageODestination;
import idawi.routing.MessageQDestination;
import idawi.routing.ComponentMatcher;
import idawi.service.ErrorLog;
import idawi.service.web.WebService;
import it.unimi.dsi.fastutil.ints.Int2LongAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.thread.Threads;
import toools.util.Date;

public class Service implements SizeOf, Serializable {

	// creates the threads that will process the messages
//	public static ExecutorService threadPool = Executors.newFixedThreadPool(1);
	public static ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	transient public final Class<? extends Service> id;
	public final Component component;
	private boolean askToRun = true;
	transient protected final List<Thread> threads = new ArrayList<>();
	transient protected final Set<AbstractOperation> operations = new HashSet<>();
	transient private final Map<String, MessageQueue> name2queue = new HashMap<>();
	transient private final Set<String> detachedQueues = new HashSet<>();
	transient final AtomicLong returnQueueID = new AtomicLong();

	// stores the number of messages received at each second
	transient final Int2LongMap second2nbMessages = new Int2LongAVLTreeMap();

	transient private long nbMsgsReceived;

	transient private Directory directory;
//	transient SD data;

	public Service(Component component) {
		this.component = component;

		if (component.lookup(getClass()) != null)
			throw new IllegalStateException("component already has service " + getClass());

		component.services.add(this);
		this.id = getClass();
		registerOperation(new descriptor());
		registerOperation(new listNativeOperations());
		registerOperation(new listOperationNames());
		registerOperation(new nbMessagesReceived());
		registerOperation(new sec2nbMessages());
		registerOperation(new shutdown());
		registerOperation(new getFriendlyName());
		registerOperation("friendlyName", q -> getFriendlyName());

		registerShortCut();
	}

	public String webShortcut() {
		return getClass().getName();
	}

	protected void registerShortCut() {
		var ws = component.lookup(WebService.class);

		if (ws != null) {
			var shortcut = webShortcut();

			var map = ws.serviceShortcuts;

			if (map != null) {
				if (map.containsKey(shortcut))
					throw new IllegalArgumentException("shortcut already in use: " + shortcut);

				map.put(shortcut, getClass().getName());
			}
		}
	}

	public class getFriendlyName extends TypedInnerClassOperation {

		public String f() {
			return getFriendlyName();
		}

		@Override
		public String getDescription() {
			return null;
		}

	}

	public class sec2nbMessages extends TypedInnerClassOperation {
		@Override
		public String getDescription() {
			return "gets a map associating a number a message received during seconds";
		}

		public Int2LongMap f() {
			return sec2nbMessages();
		}
	}

	public Int2LongMap sec2nbMessages() {
		return second2nbMessages;
	}

	public String getDescription() {
		return null;
	}

	public Directory directory() {
		if (this.directory == null) {
			this.directory = new Directory(Component.directory, "/services/" + id);
		}

		return this.directory;
	}

	public class nbMessagesReceived extends TypedInnerClassOperation {
		public long f() {
			return nbMsgsReceived;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class listOperationNames extends TypedInnerClassOperation {
		@Override
		public String getDescription() {
			return "returns the name of available operations";
		}

		public Set<String> f() {
			return new HashSet<>(operations.stream().map(o -> o.getName()).collect(Collectors.toSet()));
		}
	}

	public Set<AbstractOperation> operations() {
		return operations;
	}

	public class listNativeOperations extends TypedInnerClassOperation {
		public Set<OperationDescriptor> f() {
			return operations.stream().map(o -> o.descriptor()).collect(Collectors.toSet());
		}

		@Override
		public String getDescription() {
			return "list native operations";
		}
	}

	public String getFriendlyName() {
		return getClass().getName();
	}

	public void considerNewMessage(Message msg) {
		int sec = (int) Date.time();
		second2nbMessages.put(sec, second2nbMessages.get(sec) + 1);
		++nbMsgsReceived;

		if (msg.destination instanceof MessageODestination) {
			var dest = (MessageODestination) msg.destination;
			var operationName = dest.operationID;
			AbstractOperation operation = lookupOperation(operationName);

			if (operation == null) {
				triggerErrorHappened(dest.replyTo, new IllegalArgumentException(
						"can't find operation '" + operationName + "' in service " + getClass().getName()));
			} else {
				trigger(msg, operation, dest);
			}
		} else {
			MessageQueue q = name2queue.get(msg.destination.queueID());

			if (q == null) {
//				System.out.println("ERERROEORO");
			} else {
				q.add_sync(msg);
			}
		}
	}

	private void triggerErrorHappened(MessageQDestination replyTo, Throwable s) {
//		System.out.println(msg);
		RemoteException err = new RemoteException(s);
		logError(s);

		// report the error to the guy who asked
		if (replyTo != null) {
			component.bb().send(err, replyTo);
			component.bb().send(EOT.instance, replyTo);
		}
	}

	private synchronized void trigger(Message msg, AbstractOperation operation, MessageODestination dest) {
		var inputQ = getQueue(dest.queueID());

		// most of the time the queue will not exist, unless the user wants to use the
		// input queue of another running operation
		if (inputQ == null) {
			inputQ = createQueue(dest.queueID());
		}

		inputQ.add_sync(msg);
		final var inputQ_final = inputQ;

		Runnable r = () -> {
			try {
				final double start = Date.time();
				operation.nbCalls++;
//				Cout.debug("CALLING " + operation);
				operation.impl(inputQ_final);
//				Cout.debug("REUTNED " + operation);

				// tells the client the processing has completed
				if (dest.replyTo != null) {
					component.bb().send(EOT.instance, dest.replyTo);
				}
				operation.totalDuration += Date.time() - start;
			} catch (Throwable exception) {
				exception.printStackTrace();
				operation.nbFailures++;
				triggerErrorHappened(dest.replyTo, exception);
			}

			detachQueue(inputQ_final);
		};

		if (dest.premptive) {
			r.run();
		} else if (!threadPool.isShutdown()) {
			threadPool.submit(r);
		}
	}

	public AbstractOperation lookupOperation(String name) {
		for (var o : operations) {
			if (o.getName().equals(name)) {
				return o;
			}
		}

		return null;
	}

	public <C extends InnerClassOperation> C lookupOperation(Class<C> c) {
		for (var o : operations) {
			if (o.getClass() == c) {
				return (C) o;
			}
		}

		return null;
	}

	public <O extends InnerClassOperation> O lookup(Class<O> oc) {
//		Cout.debug(InnerOperation.serviceClass(oc));
//		Cout.debug(getClass());
		if (!InnerClassOperation.serviceClass(oc).isAssignableFrom(getClass()))
			throw new IllegalStateException(
					"searching operation " + oc.getName() + " in service class " + getClass().getName());

		for (var o : operations) {
			if (o.getClass() == oc) {
				return (O) o;
			}
		}

		return null;
	}

	public void registerOperation(String name, Operation userCode) {

		if (name == null)
			throw new NullPointerException("no name give for operation");

		registerOperation(new AbstractOperation() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getDescription() {
				return "operation " + name + " has been added programmatically (it can hence be removed)";
			}

			@Override
			public void impl(MessageQueue in) throws Throwable {
				userCode.impl(in);
			}

			@Override
			protected Class<? extends Service> getDeclaringServiceClass() {
				return Service.this.getClass();
			}
		});
	}

	public void registerOperation(String name, BiConsumer<Message, Consumer<Object>> userCode) {
		Objects.requireNonNull(name);

		registerOperation(new AbstractOperation() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public void impl(MessageQueue in) throws Throwable {
				var m = in.poll_sync();
				userCode.accept(m, r -> reply(m, r));
			}

			@Override
			protected Class<? extends Service> getDeclaringServiceClass() {
				return Service.this.getClass();
			}
		});
	}

	protected void reply(Message m, Object o) {
		var replyTo = m.destination.replyTo;
		replyTo.componentTarget =  ComponentMatcher.one(m.route.initialEmission.transport.component);
//		Cout.debugSuperVisible("reply " + o);
//		Cout.debugSuperVisible("to " + replyTo);
		component.bb().send(o, replyTo.componentTarget, replyTo.service, replyTo.queueID);
	}

	public void registerOperation(AbstractOperation o) {
		if (lookupOperation(o.getName()) != null) {
			throw new IllegalStateException(
					"in class: " + o.getDeclaringServiceClass() + ", operation name is already in use: " + o);
		}

		if (o instanceof TypedInnerClassOperation) {
			((TypedInnerClassOperation) o).service = this;
		}

		operations.add(o);
		o.service = this;
	}

	public void newThread_loop_periodic(long periodMs, Runnable r) {
		threads.add(Threads.newThread_loop_periodic(periodMs, () -> isAskedToRun(), r));
	}

	public void newThread_loop(long pauseMs, Runnable r) {
		threads.add(Threads.newThread_loop(pauseMs, () -> isAskedToRun(), r));
	}

	public void newThread_loop(Runnable r) {
		newThread_loop(0, r);
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		threads.add(t);
		t.start();
		return t;
	}

	protected void logError(String msg) {
		// System.err.println(component + "/" + id + " error: " + msg);
		component.forEachServiceOfClass(ErrorLog.class, s -> s.report(msg));
	}

	protected void logError(Throwable err) {
		// err.printStackTrace();
		component.forEachServiceOfClass(ErrorLog.class, errorLog -> errorLog.report(err));
	}

	public boolean isAskedToRun() {
		return askToRun;
	}

	public class shutdown extends TypedInnerClassOperation {
		public void f() {
			dispose();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public void dispose() {
		askToRun = false;
		threads.forEach(t -> t.interrupt());

		threads.forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException e) {
				logError(e);
			}
		});
	}

	@Override
	public String toString() {
		return component + "/" + id;
	}

	protected MessageQueue createQueue(String qid) {
		MessageQueue q = new MessageQueue(this, qid, 10);
		name2queue.put(qid, q);
		return q;
	}

	protected MessageQueue createAutoQueue(String prefix) {
		String qid = prefix + returnQueueID.getAndIncrement();
		return createQueue(qid);
	}

	protected MessageQueue getQueue(String qid) {
		return name2queue.get(qid);
	}

	public void detachQueue(MessageQueue q) {
		name2queue.remove(q.name);
		detachedQueues.add(q.name);
		q.cancelEventisation();
	}

	protected final OperationParameterList parms(Object... parms) {
		return new OperationParameterList(parms);
	}

	public class descriptor extends TypedInnerClassOperation {
		public ServiceDescriptor f() {
			return Service.this.descriptor();
		}

		@Override
		public String getDescription() {
			return "gives a descriptor on this service";
		}
	}

	public ServiceDescriptor descriptor() {
		var d = new ServiceDescriptor();
		d.clazz = id;
		d.description = getDescription();
		operations.forEach(o -> d.operations.add(o.descriptor()));
		d.nbMessagesReceived = nbMsgsReceived;
		return d;
	}

	public void apply(ServiceDescriptor sd) {
	}

	@Override
	public long sizeOf() {
		long size = 8; // component
		size += detachedQueues.size() * 8 + 8;
		size += 8; // id
		size += name2queue.size();

		for (var e : name2queue.entrySet()) {
			size += 8 + e.getKey().length() * 2 + 16;
			size += 8 + e.getValue().size() * 8 + 16;
		}

		size += 8;
		size += operations.size() * 8 + 16;

		for (var o : operations) {
			size += o.sizeOf();
		}

		return size;
	}

}
