package idawi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
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

import idawi.AsMethodOperation.OperationID;
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
	private final Map<String, Operation> name2operation = new HashMap<>();
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
		registerInMethodOperations();
//		registerInInnerClassOperations();
	}

	public static OperationID sec2nbMessages;

	@IdawiOperation
	public Int2LongMap sec2nbMessages() {
		return second2nbMessages;
	}

	public String getDescription() {
		return null;
	}
	
	public void reply(Message msg, Object r) {
		send(r, msg.requester);
	}

	private void registerInMethodOperations() {
		for (Class c : Clazz.bfs(getClass())) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.isAnnotationPresent(IdawiOperation.class)) {
					var o = new AsMethodOperation(m, this);
					registerOperation(o);

					try {
						// search for a field with the same name
						Field f = c.getDeclaredField(m.getName());

						if (f != null && f.getType() == OperationID.class) {

							if (f.getType() != AsMethodOperation.OperationID.class) {
								throw new IllegalStateException("field " + c.getName() + "." + f.getName()
										+ " should be of type " + AsMethodOperation.OperationID.class);
							}

							if ((f.getModifiers() & Modifier.STATIC) == 0) {
								throw new IllegalStateException(
										"field " + c.getName() + "." + f.getName() + " should be static");
							}

							if ((f.getModifiers() & Modifier.PUBLIC) == 0) {
								throw new IllegalStateException(
										"field " + c.getName() + "." + f.getName() + " should be public");
							}

							if ((f.getModifiers() & Modifier.FINAL) != 0) {
								throw new IllegalStateException(
										"field " + c.getName() + "." + f.getName() + " cannot not be declared final");
							}

							AsMethodOperation.OperationID id = new AsMethodOperation.OperationID(getClass(),
									m.getName());
							f.setAccessible(true);
							f.set(this, id);
						}
					} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
						throw new RuntimeException(e);
					} catch (NoSuchFieldException e) {
						// this method is not an operation, no pb
					}
				}
			}
		}
	}

	private static Field findField(Class c, String name) {
		try {
			return c.getDeclaredField(name);
		} catch (NoSuchFieldException | SecurityException e) {
			return null;
		}
	}

	protected <S> S lookupService(Class<? extends S> serviceID) {
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

	public static OperationID nbMessagesReceived;

	@IdawiOperation
	public long nbMessagesReceived() {
		return nbMsgsReceived;
	}

	public static OperationID listOperationNames;

	public Set<String> listOperationNames() {
		return new HashSet<String>(name2operation.keySet());
	}

	@IdawiOperation
	public class listNativeOperations extends InInnerClassTypedOperation {
		Set<OperationDescriptor> f() {
			return getOperations().stream().map(o -> o.descriptor()).collect(Collectors.toSet());
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
			Operation operation = getOperation(operationName);

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
		if (msg.requester != null) {
			send(err, msg.requester);
			send(EOT.instance, msg.requester);
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
				operation.accept(inputQ_final);
			} catch (Throwable exception) {
				operation.nbFailures++;
				RemoteException err = new RemoteException(exception);

				if (msg.requester != null) {
					send(err, msg.requester);
				}

				error(err);
			} finally {
				operation.totalDuration += Date.time() - start;
			}

			// tells the client the processing has completed
			if (msg.requester != null) {
				send(EOT.instance, msg.requester);
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

	private Collection<Operation> getOperations() {
		return name2operation.values();
	}

	private Operation getOperation(String name) {
		return name2operation.get(name);
	}

	public void registerOperation(String name, OperationFunctionalInterface userCode) {
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
			public void accept(MessageQueue in) throws Throwable {
				userCode.accept(in);
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
			public void accept(MessageQueue in) throws Throwable {
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
		if (name2operation.containsKey(o.getName())) {
			throw new IllegalStateException(
					"in class: " + o.getDeclaringService() + ", operation name is already in use: " + o);
		}

		name2operation.put(o.getName(), o);
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

	public static OperationID shutdown;

	@IdawiOperation
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

	public QueueAddress to(ComponentDescriptor c, OperationID operation) {
		return new QueueAddress(c, operation);
	}

	protected final OperationParameterList parms(Object... parms) {
		return new OperationParameterList(parms);
	}

	public void send(Object o, QueueAddress to) {
		new Message(o, to, null).send(component);
	}

	public RemotelyRunningOperation start(ServiceAddress target, OperationID operation, boolean expectReturn,
			Object initialInputData) {
		String inputQName = operation.operationName + "@" + Date.timeNs();
		var inputQaddress = new QueueAddress(target.getNotYetReachedExplicitRecipients(), target.service, inputQName,
				target.getMaxDistance(), target.getForwardProbability());
		return new RemotelyRunningOperation(this, inputQaddress, operation.operationName, expectReturn,
				initialInputData);
	}



	public List<Object> exec(ServiceAddress target, OperationID operation, double timeout, int nbResults,
			Object... parms) {
		return start(target, operation, true, new OperationParameterList(parms)).returnQ.setTimeout(timeout).collect().throwAnyError_Runtime()
				.resultMessages(nbResults).contents();
	}

	public static OperationID broadcast;

	public static OperationID descriptor;

	@IdawiOperation
	public ServiceDescriptor descriptor() {
		var d = new ServiceDescriptor();
		d.name = id.getName();
		d.description = getDescription();
		getOperations().forEach(o -> d.operationDescriptors.add(o.descriptor()));
		d.nbMessagesReceived = nbMsgsReceived;
		return d;
	}
}
