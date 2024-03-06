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
import java.util.stream.Collectors;

import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.MessageODestination;
import idawi.routing.MessageQDestination;
import idawi.routing.RoutingService;
import idawi.service.ErrorLog;
import idawi.service.local_view.EndpointDescriptor;
import idawi.service.local_view.ServiceInfo;
import idawi.service.web.WebService;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.util.Date;

public class Service implements SizeOf, Serializable {

	public double now() {
		return Idawi.agenda.now();
	}

	transient public final Class<? extends Service> id = getClass();
	public Component component;
	private boolean askToRun = true;
//	transient protected final List<Thread> threads = new ArrayList<>();
	transient private final Set<AbstractEndpoint> endpoints = new HashSet<>();
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
		registerURLShortCut();

		registerEndpoint("friendlyName", q -> getFriendlyName());

		registerInnerClassEndpoints();
	}

	private void registerInnerClassEndpoints() {
		for (var innerClass : getClass().getClasses()) {
			if (InnerClassEndpoint.class.isAssignableFrom(innerClass)) {
				try {
					registerEndpoint((InnerClassEndpoint) innerClass.getConstructor(innerClass.getDeclaringClass())
							.newInstance(this));
				} catch (InvocationTargetException | InstantiationException | IllegalAccessException
						| IllegalArgumentException | NoSuchMethodException | SecurityException err) {
					throw new IllegalStateException(err);
				}
			}
		}
	}

	protected RoutingService<?> routing() {
		return component.defaultRoutingProtocol();
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

	public String webShortcut() {
		return getClass().getName();
	}

	protected void registerURLShortCut() {
		var ws = component.service(WebService.class);

		if (ws != null && ws != this) {
			var shortcut = webShortcut();

			if (ws.serviceShortcuts.containsKey(shortcut))
				throw new IllegalArgumentException("shortcut already in use: " + shortcut);

			ws.serviceShortcuts.put(shortcut, getClass().getName());
		}
	}

	public class getFriendlyName extends TypedInnerClassEndpoint {

		public String f() {
			return getFriendlyName();
		}

		@Override
		public String getDescription() {
			return null;
		}
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

	public class nbMessagesReceived extends TypedInnerClassEndpoint {
		public long f() {
			return nbMsgsReceived;
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class listOperationNames extends TypedInnerClassEndpoint {
		@Override
		public String getDescription() {
			return "returns the name of available operations";
		}

		public Set<String> f() {
			return new HashSet<>(endpoints.stream().map(o -> o.getName()).collect(Collectors.toSet()));
		}
	}

	public Set<AbstractEndpoint> endpoints() {
		return endpoints;
	}

	public class listNativeOperations extends TypedInnerClassEndpoint {
		public Set<EndpointDescriptor> f() {
			return endpoints.stream().map(o -> o.descriptor()).collect(Collectors.toSet());
		}

		@Override
		public String getDescription() {
			return "list native operations";
		}
	}

	public String getFriendlyName() {
		return getClass().getName();
	}

	public void process(Message msg) {
		++nbMsgsReceived;

		if (msg.destination instanceof MessageODestination) {
			var dest = (MessageODestination) msg.destination;
			AbstractEndpoint endpoint = lookupEndpoint(dest.operationID.getSimpleName());

			if (endpoint == null) {
				triggerErrorHappened(dest.replyTo, new IllegalArgumentException("can't find endpoint '" + dest));
			} else {
				trigger(msg, endpoint, dest);
			}
		} else {
			MessageQueue q = name2queue.get(msg.destination.queueID());

			if (q == null) {
				System.err.println("ERERROEORO");
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
			component.bb().send(err, true, replyTo);
		}
	}

	private synchronized void trigger(Message msg, AbstractEndpoint endpoint, MessageODestination dest) {
		var inputQ = getQueue(dest.queueID());

		// most of the time the queue will not exist, unless the user wants to use the
		// input queue of another running endpoint
		if (inputQ == null) {
			inputQ = createQueue(dest.queueID());
		}

		inputQ.add_sync(msg);
		final var inputQ_final = inputQ;

		Event e = new Event(new PointInTime(now())) {
			@Override
			public void run() {
				try {
					final double start = Date.time();
					endpoint.nbCalls++;

					if (component.isDigitalTwin()) {
						endpoint.digitalTwin(inputQ_final);
					} else {
						endpoint.impl(inputQ_final);
					}
					endpoint.totalDuration += Date.time() - start;
				} catch (Throwable exception) {
					exception.printStackTrace();
					endpoint.nbFailures++;
					triggerErrorHappened(dest.replyTo, exception);
				}

				detachQueue(inputQ_final);
			}
		};

		if (dest.premptive) {
			e.run();
		} else if (!Idawi.agenda.threadPool.isShutdown()) {
			Idawi.agenda.schedule(e);
		} else {
			System.err.println("ignoring exec message: " + msg);
		}
	}

	public AbstractEndpoint lookupEndpoint(String name) {
		for (var o : endpoints) {
			if (o.getName().equals(name)) {
				return o;
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
				userCode.accept(m, r -> reply(m, r, true));
			}

			@Override
			protected Class<? extends Service> getDeclaringServiceClass() {
				return Service.this.getClass();
			}
		});
	}

	protected void reply(Message initialMsg, Object response, boolean eot) {
		var replyTo = initialMsg.destination.replyTo;
		replyTo.componentMatcher = ComponentMatcher.unicast(initialMsg.route.source());
//		Cout.debugSuperVisible("reply " + o);
//		Cout.debugSuperVisible("to " + replyTo);
		component.bb().send(response, eot, replyTo.componentMatcher, replyTo.service, replyTo.queueID);
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

	public class shutdown extends TypedInnerClassEndpoint {
		public void f() {
			dispose();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class size extends TypedInnerClassEndpoint {
		public long size() {
			return sizeOf();
		}

		@Override
		public String getDescription() {
			return "nb of bytes used by this knowkedge base";
		}
	}

	public void dispose() {
		askToRun = false;

		component.services.remove(this);
	}

	@Override
	public String toString() {
		return component + "/" + id.getSimpleName();
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

	public void detachQueue(MessageQueue q) {
		name2queue.remove(q.name);
		detachedQueues.add(q.name);
		q.cancelEventisation();
	}

	protected final EndpointParameterList parms(Object... parms) {
		return new EndpointParameterList(parms);
	}

	public class descriptor extends TypedInnerClassEndpoint {
		public ServiceInfo f() {
			return Service.this.descriptor();
		}

		@Override
		public String getDescription() {
			return "gives a descriptor on this service";
		}
	}

	public ServiceInfo descriptor() {
		var d = new ServiceInfo();
		d.clazz = id;
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

}
