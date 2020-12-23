package idawi;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
import toools.thread.Q;
import toools.thread.Threads;

public class Service {

	// creates the threads that will process the messages
//	public static ExecutorService threadPool = Executors.newFixedThreadPool(1);
	public static ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	public final Class<? extends Service> id;
	public final Component component;
	private boolean askToRun = true;
	protected final List<Thread> threads = new ArrayList<>();
	final Map<String, AbstractOperation> name2operation = new HashMap<>();

	private final AtomicLong returnQueueID = new AtomicLong();
	private long nbMessages;

	public Service() throws Throwable {
		this(new Component());
		run();
	}

	@Operation
	public ServiceDescriptor descriptor() {
		var d = new ServiceDescriptor();
		d.name = id.getName();
		name2operation.values().forEach(o -> d.operations.add(o.descriptor));
		return d;
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
				if (m.isAnnotationPresent(Operation.class)) {
					var o = name2operation.get(m.getName());

					if (o != null) {
						throw new IllegalStateException("operation name is already in use: " + c.getName() + "."
								+ m.getName() + " with signature " + o.descriptor().parameterTypes);
					}

					name2operation.put(m.getName(), new AbstractOperation(this, m) {
						@Override
						public String getDescription() {
							return "in method operation";
						}
					});

					try {
						Field f = c.getField(m.getName());
						/*
						 * if ((f.getModifiers() & Modifier.STATIC) == 0) { System.err.println(
						 * "warning: class " + c.getName() + ", field " + m.getName() +
						 * " should be static"); } else if ((f.getModifiers() & Modifier.PUBLIC) == 0) {
						 * System.err.println( "warning: class " + c.getName() + ", field " +
						 * m.getName() + " should be public"); }
						 * 
						 * OperationID value = new OperationID(); value.name = m.getName();
						 * System.out.println(c + "." + m.getName()); f.set(this, value);
						 */
					} catch (NoSuchFieldException e) {
//						System.err.println("warning: class " + c.getName() + ", missing: public final OperationID "
//								+ m.getName() + ";");
					} catch (IllegalArgumentException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}

	public Directory directory() {
		return new Directory(Component.directory, "/services/" + id);
	}

	@Operation
	private long nbMessagesReceived() {
		return nbMessages;
	}

	@OperationName
	public static OperationID listOperationNames;

	@Operation
	private Set<String> listOperationNames() {
		return new HashSet<String>(name2operation.keySet());
	}

	@Operation
	private void listNativeActions(Consumer out) {
		name2operation.values().forEach(o -> out.accept(o.descriptor));
	}

	public String getFriendlyName() {
		return getClass().getName();
	}

	Object2ObjectMap<Object, ChunkReceiver> chunk2buf = new Object2ObjectOpenHashMap<>();

	public void requestStream(To to, To replyTo, Object... parms) {
		call(to, new OperationParameterList(parms));
	}

	final Int2LongMap second2nbMessages = new Int2LongOpenHashMap();

	@Operation
	private Int2LongMap second2nbMessages() {
		return second2nbMessages;
	}

	public void considerNewMessage(Message msg) {
		second2nbMessages.put((int) Utils.time(), ++nbMessages);

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

		AbstractOperation operation = name2operation.get(msg.to.operationOrQueue);

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
						double start = Utils.time();

						// process the message
						operation.accept(msg, someResult -> {
							if (msg.replyTo != null) {
								send(someResult, msg.replyTo, null);
							} else {
								error("returns for queue " + msg.to.operationOrQueue + " in service  " + id
										+ " are discarded because the message specifies no return recipient");
							}
						});

						operation.totalDuration += Utils.time() - start;
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

	public void registerOperation(String name, OperationFunctionalInterface userCode) {
		if (name2operation.containsKey(name)) {
			throw new IllegalStateException("operation name is already in use: " + name);
		}

		try {
			name2operation.put(name, new AbstractOperation(userCode,
					userCode.getClass().getMethod("accept", Message.class, Consumer.class)) {

				@Override
				public String getDescription() {
					return "in lambda operation";
				}
			});
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(e);
		}
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
		return component.descriptor().friendlyName + "/" + id;
	}

	private final Map<String, MessageQueue> name2queue = new HashMap<>();

	public Set<String> actionNames() {
		return name2queue.keySet();
	}

	protected MessageQueue createQueue(String qid, Set<ComponentInfo> expectedSenders) {
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

	public To to(ComponentInfo c, Class<? extends Service> s, String operation) {
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

	public MessageQueue call(To to, OperationParameterList parms) {
		return send(parms, to);
	}

	public MessageQueue call(To to, Object... parms) {
		return call(to, new OperationParameterList(parms));
	}

	public void startOn(Set<ComponentInfo> c) {
		send(getClass(), new To(c, Service.class, "start")).collect();
	}

	public void broadcast(Object o, int range) {
		To to = new To();
		to.service = getClass();
		to.coverage = range;
		send(o, to);
	}

	public void inform(ComponentInfo p) {
		p.services.add(id.getName());
	}

}
