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

import idawi.service.ErrorLog;
import it.unimi.dsi.fastutil.ints.Int2LongAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import toools.io.file.Directory;
import toools.reflect.Clazz;
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
	private final Set<Operation> operations = new HashSet<>();
	private final Map<String, MessageQueue> name2queue = new HashMap<>();
	final AtomicLong returnQueueID = new AtomicLong();

	// stores the number of message received at each second
	final Int2LongMap second2nbMessages = new Int2LongAVLTreeMap();

	private long nbMsgsReceived;

	private Directory directory;

	public Service() {
		this(new Component());
		// run();
	}

	public Service(Component component) {
		this.component = component;
		component.services.put(getClass(), this);
		this.id = getClass();
		registerFieldsOperations();
		registerOperation(new DescriptorOperation());
	}

	protected ComponentAddress ca() {
		return new ComponentAddress(component.descriptor());
	}

	public class getFriendlyName extends InnerClassTypedOperation {
		public String f() {
			return getFriendlyName();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class sec2nbMessages extends InnerClassTypedOperation {

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

	private void registerFieldsOperations() {
		for (Class c : Clazz.bfs(getClass())) {
			for (var f : c.getDeclaredFields()) {
				if (Operation.class.isAssignableFrom(f.getType()) && f.isAnnotationPresent(IdawiOperation.class)) {
					try {
						var constructor = f.getType().getConstructor(c);
						var operation = (Operation) constructor.newInstance(this);
						f.set(this, operation);
						operations.add(operation);
					} catch (Throwable e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	protected <S extends Service> S lookupService(Class<? extends S> serviceID) {
		return component.lookupService(serviceID);
	}

	public void run() throws Throwable {
	}

	public Directory directory() {
		if (this.directory == null) {
			this.directory = new Directory(Component.directory, "/services/" + id);
		}

		return this.directory;
	}

	public class nbMessagesReceived extends InnerClassTypedOperation {
		public long f() {
			return nbMsgsReceived;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class listOperationNames extends InnerClassTypedOperation {
		@Override
		public String getDescription() {
			return "returns the name of available operations";
		}

		public Set<String> f() {
			return new HashSet<>(operations.stream().map(o -> o.getName()).collect(Collectors.toSet()));
		}
	}

	public class listNativeOperations extends InnerClassTypedOperation {
		Set<OperationDescriptor> f() {
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

		if (msg instanceof TriggerMessage) {
			var operationName = ((TriggerMessage) msg).operationName;
			Operation operation = lookupOperation(operationName);

			if (operation == null) {
				err(msg, getClass() + ": can't find operation: " + operationName);
			} else {
				trigger((TriggerMessage) msg, operation);
			}
		} else {
			MessageQueue q = getQueue(msg.to.queue);

			if (q == null) {
//				System.out.println(msg);
				err(msg, getClass() + ": can't find queue: " + msg.to.queue);
			} else {
				q.add_blocking(msg);
			}
		}
	}

	private void err(Message msg, String s) {
		System.out.println(msg);
		RemoteException err = new RemoteException(s);
		error(err);

		// report the error to the guy who asked
		if (msg.replyTo != null) {
			send(err, msg.replyTo);
			send(EOT.instance, msg.replyTo);
		}
	}

	private synchronized void trigger(TriggerMessage msg, Operation operation) {
		var sender = msg.route.get(0).component;
		var inputQ = getQueue(msg.to.queue);

		// most of the time the queue will not exist, unless the user wants to use the
		// input queue of another running operation
		if (inputQ == null) {
			inputQ = createQueue(msg.to.queue, Set.of(sender));
		}

		inputQ.add_blocking(msg);
		final var inputQ_final = inputQ;

		Runnable r = () -> {
			operation.nbCalls++;
			double start = Date.time();

			try {
//				Cout.debug(operation);
				operation.exec(inputQ_final);
			} catch (Throwable exception) {
				operation.nbFailures++;
				RemoteException err = new RemoteException(exception);

				if (msg.replyTo != null) {
					send(err, msg.replyTo);
				}

				error(err);
			} finally {
				operation.totalDuration += Date.time() - start;
			}

			// tells the client the processing has completed
			if (msg.replyTo != null) {
				send(EOT.instance, msg.replyTo);
			}

			deleteQueue(inputQ_final);
		};

		if (msg.premptive) {
			r.run();
		} else {
			if (!threadPool.isShutdown()) {
				threadPool.submit(r);
			}
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

	public <O> O lookupOperation(Class<O> c) {
		for (var o : operations) {
			if (o.getClass() == c) {
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
			protected Class<? extends Service> getDeclaringService() {
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
				return "operation " + name + " has been added programmatically (it can hence be removed)";
			}

			@Override
			public void exec(MessageQueue in) throws Throwable {
				var m = in.get_blocking();
				userCode.accept(m, r -> reply(m, r));
			}

			@Override
			protected Class<? extends Service> getDeclaringService() {
				return Service.this.getClass();
			}
		});
	}

	public void registerOperation(Operation o) {
		if (lookupOperation(o.getName()) != null) {
			throw new IllegalStateException(
					"in class: " + o.getDeclaringService() + ", operation name is already in use: " + o);
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

	protected void error(String msg) {
		// System.err.println(component + "/" + id + " error: " + msg);
		component.lookupServices(ErrorLog.class, s -> s.report(msg));
	}

	protected void error(Throwable err) {
		// err.printStackTrace();
		component.lookupServices(ErrorLog.class, s -> s.report(err));
	}

	public boolean isAskedToRun() {
		return askToRun;
	}

	public class shutdown extends InnerClassTypedOperation {
		public void f() {
			shutdown();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public void shutdown() {
		askToRun = false;
		threads.forEach(t -> t.interrupt());

		threads.forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException e) {
				error(e);
			}
		});
	}

	@Override
	public String toString() {
		return component.friendlyName + "/" + id;
	}

	protected MessageQueue createQueue(String qid, Set<ComponentDescriptor> expectedSenders) {
		MessageQueue q = new MessageQueue(qid, expectedSenders, 10, wannaDie -> deleteQueue(wannaDie));
		name2queue.put(qid, q);
		return q;
	}

	protected MessageQueue getQueue(String qid) {
		return name2queue.get(qid);
	}

	protected void deleteQueue(MessageQueue q) {
		name2queue.remove(q.name);
		q.cancelEventisation();
	}

	protected final OperationParameterList parms(Object... parms) {
		return new OperationParameterList(parms);
	}

	public void send(Object o, QueueAddress to) {
		new Message(o, to, null).send(component);
	}

	public RemotelyRunningOperation start(OperationAddress target, boolean expectReturn, Object initialInputData) {
		String inputQName = target.opid + "@" + Date.timeNs();
		var inputQaddress = new QueueAddress(target.sa, inputQName);
		return new RemotelyRunningOperation(this, inputQaddress, target.opid, expectReturn, initialInputData);
	}

	public List<Object> exec(OperationAddress target, double timeout, int nbResults, Object... parms) {
		return start(target, true, new OperationParameterList(parms)).returnQ.setTimeout(timeout).collect()
				.throwAnyError_Runtime().resultMessages(nbResults).contents();
	}

	public class DescriptorOperation extends InnerClassTypedOperation {
		public ServiceDescriptor f() {
			return Service.this.descriptor();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public ServiceDescriptor descriptor() {
		var d = new ServiceDescriptor();
		d.name = id.getName();
		d.description = getDescription();
		operations.forEach(o -> d.operationDescriptors.add(o.descriptor()));
		d.nbMessagesReceived = nbMsgsReceived;
		return d;
	}
}
