package idawi;

import java.io.IOException;
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
import idawi.service.OperationStub;
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
	private final Map<String, MessageQueue> name2queue = new HashMap<>();
	private final AtomicLong returnQueueID = new AtomicLong();

	// stores the number of message received at each second
	final Int2LongMap second2nbMessages = new Int2LongOpenHashMap();

	private long nbMessagesReceived;

	private Directory directory;

	public Service() throws Throwable {
		this(new Component());
		run();
	}

	public Service(Component component) {
		this.component = component;
		component.services.put(getClass(), this);
		this.id = getClass();
		registerOperations();
	}

	protected void reply(Message msg, Object... r) {
		send(r, msg.replyTo);
	}

	private void registerOperations() {
		for (Class innerClass : getClass().getClasses()) {
			if (innerClass.isAnnotationPresent(IdawiExposed.class)) {
				var o = (Operation) Clazz.makeInstance(innerClass);
				registerOperation(o);
			}
		}
	}

	protected <S extends OperationStandardForm> S frondEnd(Class<S> operationInnerClass, ComponentDescriptor target) {
		return frontEnd(operationInnerClass, Set.of(target));
	}

	protected <S extends OperationStandardForm> S frontEnd(Class<S> operationInnerClass,
			Set<ComponentDescriptor> target) {
		for (Class c : operationInnerClass.getClasses()) {
			if (FrontEnd.class.isAssignableFrom(c)) {
				FrontEnd fe = (FrontEnd) Clazz.makeInstance(c);
				fe.from = this;
				fe.target = target;
				return (S) fe;
			}
		}

		return null;
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

	@IdawiExposed
	public static class nbMessagesReceived extends ParameterizedOperation<Service> {
		public long f() {
			return service.nbMessagesReceived;
		}
	}

	interface listOperationNamesSig {
		Set<String> listOperationNames();
	}

	@IdawiExposed
	public class listOperationNames extends ParameterizedOperation<Service> implements listOperationNamesSig {
		@Override
		public Set<String> listOperationNames() {
			return Service.this.listOperationNames();
		}
	}

	public Set<String> listOperationNames() {
		return new HashSet<String>(name2operation.keySet());
	}

	@IdawiExposed
	private void listNativeOperations(Consumer out) {
		getOperations().forEach(o -> out.accept(o.descriptor()));
	}

	@IdawiExposed
	public String getFriendlyName() {
		return getClass().getName();
	}

	Object2ObjectMap<Object, ChunkReceiver> chunk2buf = new Object2ObjectOpenHashMap<>();

	public void requestStream(To to, To replyTo, Object... parms) {
		call(to, new OperationParameterList(parms));
	}

	@IdawiExposed
	private Int2LongMap second2nbMessages() {
		return second2nbMessages;
	}

	public void considerNewMessage(Message msg) {
		second2nbMessages.put((int) Date.time(), ++nbMessagesReceived);
		MessageQueue q = getQueue(msg.to.operationOrQueue);

		if (q == null) {
			// that's a new invocation
			if (msg.content instanceof OperationStub.InitialContent) {
				OperationStub.InitialContent ic = (OperationStub.InitialContent) msg.content;
				Operation operation = getOperation(ic.operationName);

				// but no such operation can be found
				if (operation == null) {
					err(msg, "operation not found: " + getClass().getName() + "+" + ic.operationName);
				} else {
					initiate(msg, operation);
				}
			} else {
				err(msg, "no queue and");
			}
		} else {
			q.add_blocking(msg);
		}
	}

	private void err(Message msg, String s) {
		RemoteException err = new RemoteException(s);
		error(err);

		// report the error to the guy who asked
		if (msg.replyTo != null) {
			send(err, msg.replyTo, null);
			send(EOT.instance, msg.replyTo, null);
		}
	}

	private void initiate(Message msg, Operation operation) {
		if (!threadPool.isShutdown()) {
			var sender = msg.route.get(0).component;
			var q = createQueue(msg.to.operationOrQueue, Set.of(sender));
			q.add_blocking(msg);

			threadPool.submit(() -> {
				try {
					double start = Date.time();

					// process the message
					operation.accept(q);

					operation.totalDuration += Date.time() - start;
					operation.nbCalls++;

					// tells the client the processing has completed
					if (msg.replyTo != null) {
						send(EOT.instance, msg.replyTo, null);
					}
				} catch (Throwable exception) {
					RemoteException err = new RemoteException(exception);
					// exception.printStackTrace();
					error(err);

					if (msg.replyTo != null) {
						send(err, msg.replyTo, null);
						send(EOT.instance, msg.replyTo, null);
					}
				}
			});
		}
	}

	private Collection<Operation> getOperations() {
		return name2operation.values();
	}

	private Operation getOperation(String name) {
		return name2operation.get(name);
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
			public void accept(MessageQueue in) throws Throwable {
				userCode.accept(in);
			}
		});
	}

	public void registerOperation(Operation o) {
		if (name2operation.containsKey(o.getName())) {
			throw new IllegalStateException("operation name is already in use: " + o);
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

	@IdawiExposed
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

	public Message send(Object content, To to, To returns) {
		Message msg = new Message();
		msg.to = to;
		msg.replyTo = returns;
		msg.content = content;
		send(msg);
		return msg;
	}

	public void transfer(ByteSource in, To to, To returns) throws IOException {
		in.forEachChunk(c -> send(c, to, returns));
	}

	public MessageQueue send(Object content, To to) {
		To returns = new To(Set.of(component.descriptor()), id, "queue_" + returnQueueID.getAndIncrement());
		MessageQueue returnQueue = createQueue(returns.operationOrQueue, to.notYetReachedExplicitRecipients);
		Message msg = send(content, to, returns);
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

	@IdawiExposed
	public ServiceDescriptor descriptor() {
		var d = new ServiceDescriptor();
		d.name = id.getName();
		getOperations().forEach(o -> d.operationDescriptors.add(o.descriptor()));
		d.nbMessagesReceived = nbMessagesReceived;
		return d;
	}

}
