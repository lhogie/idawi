package idawi;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.AtomicDouble;

import idawi.messaging.EOT;
import idawi.messaging.Message;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.routing.MessageODestination;
import idawi.routing.MessageQDestination;
import idawi.service.ErrorLog;
import idawi.service.local_view.EndpointDescriptor;
import idawi.service.local_view.ServiceInfo;
import idawi.service.web.WebService;
import it.unimi.dsi.fastutil.ints.Int2LongAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.thread.Threads;
import toools.util.Date;

public class Service implements SizeOf {

	// creates the threads that will process the messages
//	public static ExecutorService threadPool = Executors.newFixedThreadPool(1);
	public static ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	public static AtomicDouble time = new AtomicDouble();

	public static double now() {
		return Service.time == null ? Date.time() :Service.time.get();
		//var ts = lookup(TimeService.class);
		//return ts == null ? Date.time() : ts.now();
	}
	
	
	transient public final Class<? extends Service> id = getClass();
	public Component component;
	private boolean askToRun = true;
	transient protected final List<Thread> threads = new ArrayList<>();
	transient protected final Set<AbstractEndpoint> endpoints = new HashSet<>();
	transient private final Map<String, MessageQueue> name2queue = new HashMap<>();
	transient private final Set<String> detachedQueues = new HashSet<>();
	transient final AtomicLong returnQueueID = new AtomicLong();

	// stores the number of messages received at each second
	transient final Int2LongMap second2nbMessages = new Int2LongAVLTreeMap();

	transient private long nbMsgsReceived;

	transient private Directory directory;
//	transient SD data;

	// for deserialization
	public Service() {
	}

	public Service(Component component) {
		this.component = component;

		if (component.has(getClass()))
			throw new IllegalStateException("component already has service " + getClass());

		component.services.add(this);
		registerURLShortCut();

		registerEndpoint("friendlyName", q -> getFriendlyName());

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

	@Override
	public int hashCode() {
		return ("" + component + getClass()).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		var s = (Service) obj;
		return s.component.equals(component) && getClass().equals(s.getClass());
	}

	protected void setComponent(Component parent) {
		if (component != null) {
			component.services.remove(this);
		}

		parent.services.add(this);
		this.component = parent;
	}

	public String webShortcut() {
		return getClass().getName();
	}

	protected void registerURLShortCut() {
		var ws = component.lookup(WebService.class);

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

	public class sec2nbMessages extends TypedInnerClassEndpoint {
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

	public void considerNewMessage(Message msg) {
		int sec = (int) Date.time();
		second2nbMessages.put(sec, second2nbMessages.get(sec) + 1);
		++nbMsgsReceived;

		if (msg.destination instanceof MessageODestination) {
			var dest = (MessageODestination) msg.destination;
			AbstractEndpoint endpoint = lookupEndpoint(dest.operationID.getSimpleName());
			System.err.println(component + " trigger " + msg.route);

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
			component.bb().send(err, replyTo);
			component.bb().send(EOT.instance, replyTo);
		}
	}

	static abstract class Event implements Runnable {
		When when;
	}

	static abstract interface When extends Predicate<Event>, Serializable {
		static class Time implements When {
			public double time;

			public Time(double t) {
				this.time = t;
			}

			@Override
			public boolean test(Event t) {
				return time >= Service.time.get();
			}

		}

		public static When time(double time) {
			return new Time(time);
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

		Event r = new Event() {
			@Override
			public void run() {
				try {
					final double start = Date.time();
					endpoint.nbCalls++;
//					Cout.debug("CALLING " + endpoint);
					endpoint.impl(inputQ_final);
//					Cout.debug("REUTNED " + endpoint);

					// tells the client the processing has completed
					if (dest.replyTo != null) {
						component.bb().send(EOT.instance, dest.replyTo);
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
			r.run();
		} else if (!threadPool.isShutdown()) {
			threadPool.submit(r);
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
		replyTo.componentMatcher = ComponentMatcher.unicast(m.route.source());
//		Cout.debugSuperVisible("reply " + o);
//		Cout.debugSuperVisible("to " + replyTo);
		component.bb().send(o, replyTo.componentMatcher, replyTo.service, replyTo.queueID);
	}

	public void registerEndpoint(AbstractEndpoint o) {
		if (lookupEndpoint(o.getName()) != null) {
			throw new IllegalStateException(
					"in class: " + o.getDeclaringServiceClass() + ", endpoint name is already in use: " + o);
		}

		o.service = this;
		endpoints.add(o);
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

	public class shutdown extends TypedInnerClassEndpoint {
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
		return createQueue(prefix + returnQueueID.getAndIncrement());
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
