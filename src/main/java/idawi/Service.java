package idawi;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import idawi.messaging.ACK.eventScheduled;
import idawi.messaging.ACK.processingCompleted;
import idawi.messaging.ACK.processingStarts;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.messaging.RoutingStrategy;
import idawi.routing.ComponentMatcher;
import idawi.routing.QueueAddress;
import idawi.routing.RoutingService;
import idawi.service.ErrorLog;
import idawi.service.local_view.ServiceInfo;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.util.Date;

public class Service implements SizeOf, Serializable {

	public final Component component;
	private boolean askToRun = true;
//	transient protected final List<Thread> threads = new ArrayList<>();
	transient private final Set<AbstractEndpoint<?, ?>> endpoints = new HashSet<>();
	transient private final Map<String, MessageQueue> name2queue = new HashMap<>();
	transient private final Set<String> detachedQueues = new HashSet<>();
	transient private final AtomicLong returnQueueIDGenerator = new AtomicLong();

	transient private long nbMsgsReceived;

	transient private Directory directory;
//	transient SD data;

	public Service(Component component) {
		this.component = component;

		if (component.has(getClass()))
			throw new IllegalStateException("component already has service " + getClass());

		component.services.add(this);

		InnerClassEndpoint.registerInnerClassEndpoints(this);
	}

	@Override
	public int hashCode() {
		return ("" + component + getClass()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		var s = (Service) obj;
		return s.component.equals(component) && getClass().equals(s.getClass());
	}

	public class getFriendlyName extends SupplierEndPoint<String> {

		@Override
		public String get() {
			return getFriendlyName();
		}

		@Override
		protected String r() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public String getDescription() {
		return null;
	}

	public Directory directory() {
		if (this.directory == null) {
			this.directory = new Directory(Component.directory, "/services/" + getClass());
		}

		return this.directory;
	}

	public class nbMessagesReceived extends SupplierEndPoint<Long> {
		@Override
		public Long get() {
			return nbMsgsReceived;
		}

		@Override
		protected String r() {
			return "the number of messages received";
		}
	}

	public class listEndpoints extends SupplierEndPoint<Set<Class<? extends Endpoint<?, ?>>>> {
		@Override
		public String r() {
			return "the name of available operations";
		}

		@Override
		public Set<Class<? extends Endpoint<?, ?>>> get() {
			var r = new HashSet<Class<? extends Endpoint<?, ?>>>();
			endpoints.forEach(e -> r.add((Class<? extends Endpoint<?, ?>>) e.getClass()));
			return r;
		}

	}

	public Set<AbstractEndpoint<?, ?>> endpoints() {
		return endpoints;
	}

	public String getFriendlyName() {
		return getClass().getName();
	}

	public void process(Message msg) throws NotGrantedException {
		++nbMsgsReceived;
//Cout.debug(msg);
		AbstractEndpoint endpoint = lookupEndpoint(msg.endpointID.getSimpleName());

		if (endpoint == null)
			throw new IllegalStateException("can't find endpoint '" + msg.endpointID);

		var requester = msg.route.source();

		if (!endpoint.isGranted(requester))
			throw new NotGrantedException(endpoint.getClass(), requester);

		Event<PointInTime> e = new Event<>(new PointInTime(msg.runtimes.soonestExecTime)) {
			@Override
			public void run() {
				var inputQ = getQueue(msg.qAddr.queueID);

				try {
					final double start = now();

					// if too late
					if (start > msg.runtimes.latestExecTime)
						throw new TooLateException(this, start - msg.runtimes.latestExecTime);

					if (msg.ackReqs != null && msg.ackReqs.contains(processingStarts.class)) {
						send(new processingStarts(msg), msg.replyTo);
					}

					endpoint.nbCalls++;

					if (inputQ == null) {
						if (msg.autoCreateQueue) {
							inputQ = createQueue(msg.qAddr.queueID);
						} else {
							System.err.println("can't find queue " + msg.qAddr + " cannot deliver: " + msg.content);
						}
					}

					inputQ.add_sync(msg);

					if (component.isDigitalTwin()) {
						endpoint.digitalTwin(inputQ);
					} else {
						endpoint.impl(inputQ);
					}

					endpoint.totalDuration += Date.time() - start;

					if (msg.ackReqs != null && msg.ackReqs.contains(processingCompleted.class)) {
						send(new processingCompleted(msg), msg.replyTo);
					}

				} catch (Throwable exception) {
					endpoint.nbFailures++;
					err(msg, exception);
				} finally {
					if (msg.deleteQueueAfterCompletion) {
						deleteQueue(inputQ);
					}
				}
			}
		};

		if (msg.nbSpecificThreads == 0) {
			e.run();
		} else if (!Idawi.agenda.threadPool.isShutdown()) {
			int nbThreads = msg.nbSpecificThreads == Integer.MAX_VALUE ? Runtime.getRuntime().availableProcessors()
					: msg.nbSpecificThreads;

			for (int i = 0; i < nbThreads; ++i) {
				Idawi.agenda.schedule(e);

				if (msg.ackReqs != null && msg.ackReqs.contains(eventScheduled.class)) {
					send(new eventScheduled(msg), msg.replyTo);
				}

			}
		} else {
			System.err.println("ignoring exec message: " + msg);
		}
	}

	protected void err(Message msg, Throwable exception) {
		exception.printStackTrace();
		logError(exception);

		// report the error to the guy who asked
		send(exception, msg.replyTo);
	}

	public AbstractEndpoint lookupEndpoint(String name) {
		for (var o : endpoints) {
			if (o.getName().equals(name)) {
				return o;
			}
		}

		return null;
	}

	public <E extends Endpoint> E lookupEndpoint(Class<E> c) {
		for (var o : endpoints) {
			if (o.getClass() == c) {
				return (E) o;
			}
		}

		return null;
	}

	public void registerEndpoint(String name, Endpoint userCode) {
		if (name == null)
			throw new NullPointerException("no name give for operation");

		registerEndpoint(new AbstractEndpoint() {

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

	public void registerEndpoint(String name, BiConsumer<Message, Consumer<Object>> userCode) {
		Objects.requireNonNull(name);

		registerEndpoint(new AbstractEndpoint() {

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
				userCode.accept(m, r -> send(r, m.replyTo));
			}

			@Override
			protected Class<? extends Service> getDeclaringServiceClass() {
				return Service.this.getClass();
			}
		});
	}

	public void registerEndpoint(AbstractEndpoint o) {
		if (lookupEndpoint(o.getName()) != null) {
			throw new IllegalStateException(
					"in class: " + o.getDeclaringServiceClass() + ", endpoint name is already in use: " + o);
		}

		o.service = this;
		endpoints.add(o);
	}

	protected void logError(String msg) {
		// System.err.println(component + "/" + id + " error: " + msg);
		component.forEachService(ErrorLog.class, s -> s.report(msg));
	}

	protected void logError(Throwable err) {
		// err.printStackTrace();
		component.forEachService(ErrorLog.class, errorLog -> errorLog.report(err));
	}

	public boolean isAskedToRun() {
		return askToRun;
	}

	public class shutdown extends InnerClassEndpoint {

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			dispose();
		}
	}

	public class retrieveQ extends InnerClassEndpoint {

		@Override
		public String getDescription() {
			return "returns the full content of a queue";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var tmsg = in.poll_sync();
			var qid = (String) tmsg.content;
			getQueue(qid).toList();
		}
	}
	
	public class endpointInputSpecs extends FunctionEndPoint<Class<Endpoint>, Class> {		
		@Override
		public String getDescription() {
			return "returns input spec of an endpoint";
		}

		@Override
		public Class f(Class<Endpoint> e) throws Throwable {
			return Endpoint.inputSpecification(e);
		}
	}

	public class size extends InnerClassEndpoint {

		@Override
		public String getDescription() {
			return "nb of bytes used by this service";
		}

		@Override
		public void impl(MessageQueue in) {
			reply(Service.this.sizeOf(), in.poll_sync());
		}
	}

	public class deliverToQueue extends InnerClassEndpoint {
		public long nbDelivery;

		@Override
		public String getDescription() {
			return "do nothing";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			++nbDelivery;
		}
	}

	public void dispose() {
		askToRun = false;

		component.services.remove(this);
	}

	@Override
	public String toString() {
		return component + "/" + getClass().getSimpleName();
	}

	protected MessageQueue createQueue(String qid) {
		MessageQueue q = new MessageQueue(this, qid, 10);
		name2queue.put(qid, q);
		return q;
	}

	protected MessageQueue createUniqueQueue(String prefix) {
		return createQueue(prefix + returnQueueIDGenerator.getAndIncrement());
	}

	protected MessageQueue getQueue(String qid) {
		return name2queue.get(qid);
	}

	public void deleteQueue(MessageQueue q) {
		name2queue.remove(q.name);
		detachedQueues.add(q.name);
		q.cancelEventisation();
	}

	public ServiceInfo descriptor() {
		var d = new ServiceInfo();
		d.clazz = getClass();
		d.description = getDescription();
		endpoints.forEach(o -> d.endpoints.add(o.descriptor()));
		d.nbMessagesReceived = nbMsgsReceived;
		d.nbQueues = name2queue.size();
		d.sizeOf = sizeOf();
		return d;
	}

	public void apply(ServiceInfo sd) {
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
		size += endpoints.size() * 8 + 16;

		for (var o : endpoints) {
			size += o.sizeOf();
		}

		return size;
	}

	public double now() {
		return Idawi.agenda.time();
	}

	private <I> Computation prepareExec(Consumer<Message<I>> privateCustomizer, Consumer<Message<I>> userCustomizer) {
		var msg = new Message<I>();
		var r = component.defaultRoutingProtocol();
		var dd = r.defaultData();
		msg.routingStrategy = new RoutingStrategy(r, dd);
		var returnQ = createUniqueQueue("return-");
		msg.replyTo = new QueueAddress(ComponentMatcher.unicast(component), getClass(), returnQ.name);
		privateCustomizer.accept(msg);

		if (userCustomizer != null) {
			userCustomizer.accept(msg);
		}

		r.accept(msg);
		return new Computation(msg.qAddr, returnQ);
	}

	public <S extends Service, I, O, E extends InnerClassEndpoint<I, O>> Computation exec(ComponentMatcher t,
			Class<S> s, Class<E> e, Consumer<Message<I>> msgCustomizer) {
		return prepareExec(msg -> {
			msg.qAddr = new QueueAddress(t, s, e.getSimpleName() + "@" + now());
			msg.autoCreateQueue = true;
			msg.deleteQueueAfterCompletion = true;
			msg.endpointID = e;
			
			var iSpec = Endpoint.inputSpecification(e);
			
			if (iSpec != null) {
				try {
					msg.content = iSpec.getConstructor().newInstance();
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException err) {
//					throw new IllegalStateException(err);
				}
			}
		}, msgCustomizer);
	}

	public <S extends Service, I, O, E extends InnerClassEndpoint<I, O>> Computation exec(Component c,
			Class<S> service, Class<E> e, Consumer<Message<I>> msgCustomizer) {
		return exec(ComponentMatcher.unicast(c), service, e, msgCustomizer);
	}

	public Computation send(Object content, QueueAddress dest) {
		return send(content, dest, msg -> msg.eot = false);
	}

	public <I> Computation send(I content, QueueAddress dest, Consumer<Message<I>> msgCustomizer) {
		return prepareExec(msg -> {
			msg.qAddr = dest;
			msg.endpointID = deliverToQueue.class;
			msg.content = content;
		}, msgCustomizer);
	}

	protected <I> Computation send(Consumer<Message<I>> msgCustomizer) {
		return prepareExec(msg -> msg.endpointID = deliverToQueue.class, msgCustomizer);
	}

	public static double RPC_TIMEOUT = 5;

	public <S extends Service, I, O, E extends InnerClassEndpoint<I, O>> Object exec_rpc(Component to,
			Class<S> service, Class<E> e, Consumer<Message<I>> c) {

		try {
			var rc = exec(to, service, e, c);
			return rc.returnQ.poll_sync(RPC_TIMEOUT).throwIfError().content;
		} catch (IllegalArgumentException | SecurityException err) {
			throw new IllegalStateException(err);
		}
	}

}
