package idawi;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import idawi.net.NetworkingService;
import idawi.service.ErrorLog;
import toools.thread.Q;
import toools.thread.Threads;

public class Service {

	// creates the threads that will process the messages
	public static ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	public final Class<? extends Service> id;
	public final Component component;
	private boolean askToRun = true;
	protected final List<Thread> threads = new ArrayList<>();
	final Map<String, AbstractOperation> name2operation = new HashMap<>();

	private final AtomicLong returnQueueID = new AtomicLong();
	private long nbMessages;

	public Service(Component t) {
		this.component = t;
		t.services.put(getClass(), this);
		this.id = getClass();

		for (Method m : getClass().getMethods()) {
			if (m.isAnnotationPresent(Operation.class)) {
				name2operation.put(m.getName(), new InMethodOperation(this, m));
			}
		}
	}

	@Operation
	public long nbMessagesReceived() {
		return nbMessages;
	}

	@Operation
	public Set<String> listNativeActions() {
		return new HashSet<String>(name2operation.keySet());
	}

	public String getFriendlyName() {
		return getClass().getName();
	}

	public void considerNewMessage(Message msg) {
		++nbMessages;
		AbstractOperation operation = name2operation.get(msg.to.operationOrQueue);

		// this queue is not associated to any processing, to leave in a queue and some
		// thread will pick it up later
		if (operation == null) {
			Q<Message> q = getQueue(msg.to.operationOrQueue);

			// no queue, it has already expired
			if (q == null) {
				name2queue.keySet().forEach(n -> System.out.println(n));

				if (msg.replyTo != null) {
					MessageException err = new MessageException("operation/queue '" + msg.to.operationOrQueue
							+ "' not existing on service " + getClass().getName());
					error(err);
					send(err, msg.replyTo, null);
					send(new EOT(), msg.replyTo, null);
				}
			} else {
				q.add_blocking(msg);
			}
		} else {
			threadPool.submit(() -> {
				try {
					// process the message
					operation.accept(msg, someResult -> {
						if (msg.replyTo != null) {
							if (someResult instanceof InputStream) {
								throw new IllegalStateException("streams are not yet supported");
//								Streams.stream((InputStream) someResult, this, msg.returnTarget);
							} else {
								send(someResult, msg.replyTo, null);
							}
						} else {
							error("returns for queue " + msg.to.operationOrQueue + " in service  " + id
									+ " are discarded because the message specifies no return recipient");
						}
					});

					// tells the client the processing has completed
					if (msg.replyTo != null) {
						send(new EOT(), msg.replyTo, null);
					}
				} catch (Throwable exception) {
					MessageException err = new MessageException(exception);
					exception.printStackTrace();
					error(err);

					if (msg.replyTo != null) {
						send(err, msg.replyTo, null);
						send(new EOT(), msg.replyTo, null);
					}
				}
			});
		}
	}

	@Operation
	public Object callRESTOperation(Set<ComponentInfo> components, Class<? extends Service> serviceID, String operation,
			String... stringParms) throws MessageException {
		AbstractOperation m = name2operation.get(operation);

		if (!InMethodOperation.class.isInstance(m)) {
			throw new MessageException("operation " + operation + " is not implemented as a method");
		}

		Object[] actualParms = new Object[stringParms.length];
		Class<?>[] types = m.signature().parameterTypes;

		for (int i = 0; i < stringParms.length; ++i) {
			actualParms[i] = fromString(stringParms[i], types[i]);
		}

		try {
			return ((InMethodOperation) m).method.invoke(this, actualParms);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new MessageException(e);
		}
	}

	private Object fromString(String from, Class<?> to) throws MessageException {
		if (to == String.class) {
			return from;
		} else if (to == double.class || to == Double.class) {
			return Double.valueOf(from);
		} else if (to == int.class || to == Integer.class) {
			return Long.valueOf(from);
		} else if (to == long.class || to == Long.class) {
			return Long.valueOf(from);
		} else if (to == int.class || to == Integer.class) {
			return Integer.valueOf(from);
		} else {
			throw new MessageException("string cannot be converted to " + to.getClass());
		}
	}

	public void registerOperation(String queue, OperationFunctionalInterface userCode) {
		name2operation.put(queue, new InLambdaOperation(userCode));
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
		err.printStackTrace();
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
		name2queue.keySet().forEach(n -> System.out.println(n));
		System.out.println("ending listing queues");

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

	public MessageQueue send(Object content, To to) {
		To returns = new To(Set.of(component.descriptor()), id, "queue_" + returnQueueID.getAndIncrement());
		MessageQueue returnQueue = createQueue(returns.operationOrQueue, to.notYetReachedExplicitRecipients);
		send(content, to, returns);
		return returnQueue;
	}

	public MessageQueue call(To to, OperationParameterList parms) {
		return send(parms, to);
	}

	public MessageQueue call(Set<ComponentInfo> descriptors, Class<? extends Service> service, String operation,
			Object... parms) {
		return call(new To(descriptors, service, operation), new OperationParameterList(parms));
	}

	public MessageQueue call(ComponentInfo descriptor, Class<? extends Service> service, String operation,
			Object... parms) {
		return call(new To(descriptor, service, operation), new OperationParameterList(parms));
	}

	public MessageQueue call(Component target, Class<? extends Service> service, String operation, Object... parms) {
		return call(new To(target.descriptor(), service, operation), new OperationParameterList(parms));
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
		p.servicesStrings.add(id.getName());
	}

}
