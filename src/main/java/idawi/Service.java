package idawi;

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

import idawi.MessageQueue.Enough;
import idawi.service.ErrorLog;
import idawi.service.PredicateRunner;
import idawi.service.PredicateRunner.SerializablePredicate;
import idawi.service.ServiceManager;
import it.unimi.dsi.fastutil.ints.Int2LongAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import toools.io.Cout;
import toools.io.file.Directory;
import toools.thread.Threads;
import toools.util.Date;

public class Service {

	// creates the threads that will process the messages
//	public static ExecutorService threadPool = Executors.newFixedThreadPool(1);
	public static ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	public final Class<? extends Service> id;
	public final Component component;
	private boolean askToRun = true;
	protected final List<Thread> threads = new ArrayList<>();
	protected final Set<Operation> operations = new HashSet<>();
	private final Map<String, MessageQueue> name2queue = new HashMap<>();
	private final Set<String> deletedQueues = new HashSet<>();
	final AtomicLong returnQueueID = new AtomicLong();

	// stores the number of message received at each second
	final Int2LongMap second2nbMessages = new Int2LongAVLTreeMap();

	private long nbMsgsReceived;

	private Directory directory;

	public Service(Component component) {
		this.component = component;
		component.services.put(getClass(), this);
		this.id = getClass();
		registerOperation(new DescriptorOperation());
		registerOperation(new listNativeOperations());
		registerOperation(new listOperationNames());
		registerOperation(new nbMessagesReceived());
		registerOperation(new sec2nbMessages());
		registerOperation(new shutdown());
		registerOperation(new getFriendlyName());
		registerOperation("friendlyName", q -> getFriendlyName());
	}

	protected To ca() {
		return new To(component.descriptor());
	}

	public class getFriendlyName extends TypedInnerOperation {

		public String f() {
			return getFriendlyName();
		}

		@Override
		public String getDescription() {
			return null;
		}

	}

	public class sec2nbMessages extends TypedInnerOperation {
		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
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

	public void reply(Message msg, Object r) {
		send(r, msg.replyTo);
	}

	public Directory directory() {
		if (this.directory == null) {
			this.directory = new Directory(Component.directory, "/services/" + id);
		}

		return this.directory;
	}

	public class nbMessagesReceived extends TypedInnerOperation {
		public long f() {
			return nbMsgsReceived;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class listOperationNames extends TypedInnerOperation {
		@Override
		public String getDescription() {
			return "returns the name of available operations";
		}

		public Set<String> f() {
			return new HashSet<>(operations.stream().map(o -> o.getName()).collect(Collectors.toSet()));
		}
	}

	public class listNativeOperations extends TypedInnerOperation {
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
		//Cout.debug(msg);
		int sec = (int) Date.time();
		second2nbMessages.put(sec, second2nbMessages.get(sec) + 1);
		++nbMsgsReceived;

		if (msg instanceof TriggerMessage) {
			var operationName = ((TriggerMessage) msg).operationName;
			Operation operation = lookupOperation(operationName);

			if (operation == null) {
				triggerErrorHappened(msg,
						new IllegalArgumentException("can't find operation '" + operationName + "' in service " + getClass().getName()));
			} else {
				trigger((TriggerMessage) msg, operation);
			}
		} else {
			MessageQueue q = name2queue.get(msg.to.queueName);

			if (q == null) {
//				System.out.println("ERERROEORO");
			} else {
				q.add_blocking(msg);
			}
		}
	}

	private void triggerErrorHappened(Message triggerMsg, Throwable s) {
//		System.out.println(msg);
		RemoteException err = new RemoteException(s);
		logError(err);

		// report the error to the guy who asked
		if (triggerMsg.replyTo != null) {
			send(err, triggerMsg.replyTo);
			send(EOT.instance, triggerMsg.replyTo);
		}
	}

	private synchronized void trigger(TriggerMessage msg, Operation operation) {
		var inputQ = getQueue(msg.to.queueName);

		// most of the time the queue will not exist, unless the user wants to use the
		// input queue of another running operation
		if (inputQ == null) {
			inputQ = createQueue(msg.to.queueName);
		}

		inputQ.add_blocking(msg);
		final var inputQ_final = inputQ;

		Runnable r = () -> {
			operation.nbCalls++;
			double start = Date.time();

			try {
//				Cout.debug(operation);
				operation.exec(inputQ_final);

				// tells the client the processing has completed
				if (msg.replyTo != null) {
					send(EOT.instance, msg.replyTo);
				}
			} catch (Throwable exception) {
				operation.nbFailures++;
				triggerErrorHappened(msg, exception);
			} finally {
				operation.totalDuration += Date.time() - start;
			}

			deleteQueue(inputQ_final);
		};

		if (msg.premptive) {
			r.run();
		} else if (!threadPool.isShutdown()) {
			threadPool.submit(r);
		}
	}

	public Operation lookupOperation(String name) {
		for (var o : operations) {
			if (o.getName().equals(name)) {
				return o;
			}
		}

		return null;
	}

	public <O extends InnerOperation> O lookup(Class<O> oc) {
		if (InnerOperation.serviceClass(oc) != getClass())
			throw new IllegalStateException("searching operation " + oc.getName() +  " in service class " + getClass().getName());
		
		for (var o : operations) {
			if (o.getClass() == oc) {
				return (O) o;
			}
		}

		return null;
	}

	public void registerOperation(String name, OperationFunctionalInterface userCode) {

		if (name == null)
			throw new NullPointerException("no name give for operation");

		registerOperation(new Operation() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getDescription() {
				return "operation " + name + " has been added programmatically (it can hence be removed)";
			}

			@Override
			public void exec(MessageQueue in) throws Throwable {
				userCode.exec(in);
			}

			@Override
			protected Class<? extends Service> getDeclaringServiceClass() {
				return Service.this.getClass();
			}
		});
	}

	public void registerOperation(String name, BiConsumer<Message, Consumer<Object>> userCode) {
		Objects.requireNonNull(name);

		registerOperation(new Operation() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public void exec(MessageQueue in) throws Throwable {
				var m = in.get_blocking();
				userCode.accept(m, r -> reply(m, r));
			}

			@Override
			protected Class<? extends Service> getDeclaringServiceClass() {
				return Service.this.getClass();
			}
		});
	}

	public void registerOperation(Operation o) {
		if (lookupOperation(o.getName()) != null) {
			throw new IllegalStateException(
					"in class: " + o.getDeclaringServiceClass() + ", operation name is already in use: " + o);
		}

		if (o instanceof TypedInnerOperation) {
			((TypedInnerOperation) o).service = this;
		}

		operations.add(o);
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

	public class shutdown extends TypedInnerOperation {
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
		return component.name + "/" + id;
	}

	protected MessageQueue createQueue(String qid) {
		MessageQueue q = new MessageQueue(this, qid, 10);
		name2queue.put(qid, q);
		return q;
	}

	protected MessageQueue createQueue() {
		String qid = "q" + returnQueueID.getAndIncrement();
		return createQueue(qid);
	}

	protected MessageQueue getQueue(String qid) {
		return name2queue.get(qid);
	}

	protected void deleteQueue(MessageQueue q) {
		name2queue.remove(q.name);
		deletedQueues.add(q.name);
		q.cancelEventisation();
	}

	protected final OperationParameterList parms(Object... parms) {
		return new OperationParameterList(parms);
	}

	public void send(Object o, QueueAddress to) {
		var m = new Message(o, to, null);
		m.originService = this.getClass().getName();
		m.send(component);
	}

	public RemotelyRunningOperation exec(OperationAddress target, MessageQueue rq, Object initialInputData) {
		String remoteQid = target.opid + "@" + Long.toHexString(Date.timeNs());
		var remoteInputQaddr = new QueueAddress(target.sa, remoteQid);
		return new RemotelyRunningOperation(this, remoteInputQaddr, target.opid, rq, initialInputData);
	}

	public RemotelyRunningOperation exec(OperationAddress target, boolean createQueue, Object initialInputData) {
		return exec(target, createQueue ? createQueue() : null, initialInputData);
	}

	public List<Object> execf(OperationAddress target, double timeout, int nbResults, Object... parms) {
		return exec(target, createQueue(), new OperationParameterList(parms)).returnQ.collect(timeout)
				.throwAnyError_Runtime().resultMessages(nbResults).contents();
	}

	public class DescriptorOperation extends TypedInnerOperation {
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
		d.name = id.getName();
		d.description = getDescription();
		operations.forEach(o -> d.operations.add(o.descriptor()));
		d.nbMessagesReceived = nbMsgsReceived;
		return d;
	}

	public Set<ComponentDescriptor> whoHasService(To to, Class<? extends Service> serviceID) {
		// we'll store herein the components that expose the given service
		Set<ComponentDescriptor> r = new HashSet<>();

		// asks the ServiceManager on all components in "to" if they they have that
		// service
		exec(to.o(ServiceManager.has.class), createQueue(), serviceID).returnQ.forEach(msg -> {
			// if this component claims he has
			if ((boolean) msg.content) {
				r.add(msg.route.source().component);
			}

			// don't quit not, other components may reply
			return Enough.no;
		});

		return r;
	}

	public <O extends InnerOperation> Set<ComponentDescriptor> test(To to, Class<O> predicate, Object... parms) {
		Set<ComponentDescriptor> r = new HashSet<>();

		exec(to.o(predicate), createQueue(), new OperationParameterList(parms)).returnQ.forEach(msg -> {
			if ((boolean) msg.content) {
				r.add(msg.route.source().component);
			}

			return Enough.no;
		});

		return r;
	}

	public Set<ComponentDescriptor> test(To to, SerializablePredicate p) {
		Set<ComponentDescriptor> r = new HashSet<>();

		exec(to.o(PredicateRunner.Test.class), createQueue(), p).returnQ.forEach(msg -> {
			if ((boolean) msg.content) {
				r.add(msg.route.source().component);
			}

			return Enough.no;
		});

		return r;
	}

}
