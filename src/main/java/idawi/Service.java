package idawi;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import idawi.net.NetworkingService;
import idawi.service.ErrorLog;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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

	private final AtomicLong returnQueueID = new AtomicLong();

	@ExposedOperation
	private long nbMessagesReceived;

	public Service() throws Throwable {
		this(new Component());
		run();
	}

	protected <S> S service(Class<? extends S> serviceID) {
		return component.lookupService(serviceID);
	}

	public void run() throws Throwable {
	}

	public Service(Component component) {
		this.component = component;
		component.services.put(getClass(), this);
		this.id = getClass();
		registerInMethodOperations();
	}

	private void registerInMethodOperations() {
		for (Class c : Clazz.getClasses2(getClass())) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.isAnnotationPresent(ExposedOperation.class)) {
					registerOperation(new InMethodOperation(this, m));
				}
			}
		}
	}

	public void registerInFieldOperations() {
		for (Field field : getClass().getFields()) {
			if (field.isAnnotationPresent(ExposedOperation.class)) {
				Object v = get(field);

				if (v instanceof OperationField) {
					var of = (OperationField) v;
					of.name = field.getName();
					registerOperation(of);
				} else {
					InFieldOperation fi = InFieldOperation.toOperation(field, v);

					if (fi == null) {
						throw new IllegalStateException(
								"don't know what to do with field " + getClass().getName() + "." + field.getName());
					}

					registerOperation(fi);
				}
			}
		}

		fieldOperationScanned = true;
	}

	private Object get(Field field) {
		try {
			return field.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public Directory directory() {
		return new Directory(Component.directory, "/services/" + id);
	}

	@ExposedOperation
	private long nbMessagesReceived() {
		return nbMessagesReceived;
	}

	@OperationName
	public static OperationID listOperationNames;

	@ExposedOperation
	private Set<String> listOperationNames() {
		return new HashSet<String>(name2operation.keySet());
	}

	@ExposedOperation
	private void listNativeOperations(Consumer out) {
		getOperations().forEach(o -> out.accept(o.descriptor()));
	}

	@ExposedOperation
	public String getFriendlyName() {
		return getClass().getName();
	}

	Object2ObjectMap<Object, ChunkReceiver> chunk2buf = new Object2ObjectOpenHashMap<>();

	public void requestStream(To to, To replyTo, Object... parms) {
		call(to, new OperationParameterList(parms));
	}

	final Int2LongMap second2nbMessages = new Int2LongOpenHashMap();

	private boolean fieldOperationScanned = false;

	@ExposedOperation
	private Int2LongMap second2nbMessages() {
		return second2nbMessages;
	}

	public void considerNewMessage(Message msg) {
		second2nbMessages.put((int) Date.time(), ++nbMessagesReceived);

		if (msg.content instanceof Chunk) {
			Chunk chunk = (Chunk) msg.content;
			ChunkReceiver receiver = chunk2buf.get(chunk.id);

			if (receiver == null) {
//				chunk2buf.put(chunk.id, receiver = new ChunkReceiver(chunk.end));
			}

//			receiver.addChunk(chunk, msg.route);

			if (receiver.hasCompleteData()) {
				chunk2buf.remove(chunk.id);
				Message m = new Message();
				m.content = receiver;
				considerNewMessage(m);
			}

			return;
		}

		Operation operation = getOperation(msg.to.operationOrQueue);

		// this queue is not associated to any processing, to leave in a queue and some
		// thread will pick it up later
		if (operation == null) {
			MessageQueue q = getQueue(msg.to.operationOrQueue);

			// no queue, it has already expired
			if (q == null) {
				if (msg.replyTo != null) {
					RemoteException err = new RemoteException("operation/queue '" + msg.to.operationOrQueue
							+ "' not existing on service " + getClass().getName());
					error(err);
					send(err, msg.replyTo, null);
					send(new EOT(), msg.replyTo, null);
				}
			} else {
				q.add_blocking(msg);
			}
		} else {
			if (!threadPool.isShutdown()) {
				threadPool.submit(() -> {
					try {
						double start = Date.time();

						// process the message
						operation.accept(msg, someResult -> {
							if (msg.replyTo != null) {
								send(someResult, msg.replyTo, null);
							} else {
								error("returns for queue " + msg.to.operationOrQueue + " in service  " + id
										+ " are discarded because the message specifies no return recipient");
							}
						});

						operation.totalDuration += Date.time() - start;
						operation.nbCalls++;

						// tells the client the processing has completed
						if (msg.replyTo != null) {
							send(new EOT(), msg.replyTo, null);
						}
					} catch (Throwable exception) {
						RemoteException err = new RemoteException(exception);
						// exception.printStackTrace();
						error(err);

						if (msg.replyTo != null) {
							send(err, msg.replyTo, null);
							send(new EOT(), msg.replyTo, null);
						}
					}
				});
			}
		}
	}

	private Collection<Operation> getOperations() {
		ensureInFieldOperationsScanned();
		return name2operation.values();
	}

	private Operation getOperation(String name) {
		ensureInFieldOperationsScanned();
		return name2operation.get(name);
	}

	@ExposedOperation
	private void ensureInFieldOperationsScanned() {
		if (!fieldOperationScanned) {
			registerInFieldOperations();
		}
	}

	public void registerOperation(String name, OperationStandardForm userCode) {
		registerOperation(new Operation(getClass()) {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public String getDescription() {
				return "operation " + name + " has been added programmatically (it can hence be removed)";
			}

			@Override
			public void accept(Message msg, Consumer<Object> returns) throws Throwable {
				userCode.accept(msg, returns);
			}
		});
	}

	public void registerOperation(Operation o) {
		if (name2operation.containsKey(o.getName())) {
			throw new IllegalStateException("operation name is already in use: " + o.getName());
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
		System.err.println(component + "/" + id + " error: " + msg);
		component.lookupServices(ErrorLog.class, s -> s.report(msg));
	}

	protected void error(Throwable err) {
		// err.printStackTrace();
		component.lookupServices(ErrorLog.class, s -> s.report(err));
	}

	public boolean isAskedToRun() {
		return askToRun;
	}

	@ExposedOperation
	public void shutdown() {
		askToRun = false;
		threads.forEach(t -> t.interrupt());

		threads.forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public String toString() {
		return component.friendlyName + "/" + id;
	}

	private final Map<String, MessageQueue> name2queue = new HashMap<>();

	protected MessageQueue createQueue(String qid, Set<ComponentDescriptor> expectedSenders) {
		MessageQueue q = new MessageQueue(qid, expectedSenders, 10, wannaDie -> delete(wannaDie));
		name2queue.put(qid, q);
		return q;
	}

	protected MessageQueue getQueue(String qid) {
		return name2queue.get(qid);
	}

	protected void delete(MessageQueue q) {
		name2queue.remove(q.name);
		q.cancelEventisation();
	}

	public To to(ComponentDescriptor c, Class<? extends Service> s, String operation) {
		return new To(c, s, operation);
	}

	// sends a message and don't wait for any return
	public void send(Message msg) {
		component.lookupService(NetworkingService.class).send(msg);
	}

	public void send(Object content, To to, To returns) {
		Message msg = new Message();
		msg.to = to;
		msg.replyTo = returns;
		msg.content = content;
		send(msg);
	}

	public void transfer(ByteSource in, To to, To returns) throws IOException {
		in.forEachChunk(c -> send(c, to, returns));
	}

	public MessageQueue send(Object content, To to) {
		To returns = new To(Set.of(component.descriptor()), id, "queue_" + returnQueueID.getAndIncrement());
		MessageQueue returnQueue = createQueue(returns.operationOrQueue, to.notYetReachedExplicitRecipients);
		send(content, to, returns);
		return returnQueue;
	}

	public MessageQueue call(To to, Object... parms) {
		return send(new OperationParameterList(parms), to);
	}

	public void startOn(Set<ComponentDescriptor> c) {
		send(getClass(), new To(c, Service.class, "start")).collect();
	}

	public void broadcast(Object o, int range) {
		To to = new To();
		to.service = getClass();
		to.coverage = range;
		send(o, to);
	}

	@ExposedOperation
	public ServiceDescriptor descriptor() {
		var d = new ServiceDescriptor();
		d.name = id.getName();
		getOperations().forEach(o -> d.operationDescriptors.add(o.descriptor()));
		d.nbMessagesReceived = nbMessagesReceived;
		return d;
	}

}
