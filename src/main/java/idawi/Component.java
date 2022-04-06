package idawi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import idawi.net.LMI;
import idawi.net.NetworkingService;
import idawi.net.TransportLayer;
import idawi.routing.RoutingScheme_bcast;
import idawi.service.Bencher;
import idawi.service.DemoService;
import idawi.service.DeployerService;
import idawi.service.ErrorLog;
import idawi.service.ExternalCommandsService;
import idawi.service.FileService;
import idawi.service.JVMInfo;
import idawi.service.PingService;
import idawi.service.ServiceManager;
import idawi.service.SystemMonitor;
import idawi.service.rest.WebServer;
import toools.io.file.Directory;

public class Component {
	public static final Directory directory = new Directory("$HOME/" + Component.class.getPackage().getName());
	public static final ConcurrentHashMap<String, Component> componentsInThisJVM = new ConcurrentHashMap<>();

	public String name;
	private ComponentDescriptor descriptor;
	final Map<Class<? extends Service>, Service> services = new HashMap<>();
	public final Set<ComponentDescriptor> otherComponentsSharingFilesystem = new HashSet<>();
	public final Set<Component> killOnDeath = new HashSet<>();
	public ComponentDescriptor parent;

	public Component() {
		this("c" + componentsInThisJVM.size());
	}

	public Component(String name) {
		this(ComponentDescriptor.fromName(name));
	}

	public Component(ComponentDescriptor descriptor) {
		if (componentsInThisJVM.containsKey(descriptor.name)) {
			throw new IllegalStateException(descriptor.name + " is already in use in this JVM");
		}

		this.name = descriptor.name;

		// start basic services
		new ServiceManager(this);
		new SystemMonitor(this);
		new DeployerService(this);
		new PingService(this);
		new Bencher(this);
//		new RoutingScheme1(this);
		new RoutingScheme_bcast(this);
		new ErrorLog(this);
		new DemoService(this);
		new WebServer(this);
		new ExternalCommandsService(this);
		new FileService(this);
		new RegistryService(this);
		new NetworkingService(this);
		new JVMInfo(this);

		this.descriptor = createDescriptor();

		// descriptorRegistry.add(descriptor());
		componentsInThisJVM.put(descriptor.name, this);
	}

	public void dispose() {
		killOnDeath.forEach(t -> t.dispose());
		componentsInThisJVM.remove(this);
	}

	public Collection<Service> services() {
		return services.values();
	}

	public void forEachService(Consumer<Service> c) {
		services.values().forEach(s -> c.accept(s));
	}

	public <S extends Service> S lookup(Class<S> id) {
		return (S) services.get(id);
	}

	public <O extends InnerOperation> O operation(Class<O> id) {
		var serviceClass = InnerOperation.serviceClass(id);
		var service = lookup(serviceClass);
		return service.lookup(id);
	}

	public void forEachService(Predicate<Service> predicate, Consumer<Service> h) {
		forEachService(service -> {
			if (predicate.test(service)) {
				h.accept((Service) service);
			}
		});
	}

	public <S extends Service> void forEachServiceOfClass(Class<S> serviceID, Consumer<S> h) {
		forEachService(s -> serviceID.isInstance(s), s -> h.accept((S) s));
	}

	public <S extends InnerOperation> S lookupOperation(Class<? extends S> c) {
		var sc = (Class<? extends Service>) c.getDeclaringClass();
		var service = lookup(sc);

		if (service == null)
			throw new IllegalArgumentException("service " + sc.getName() + " cannot be found");

		var o = service.lookup(c);
		return o;
	}

	public Service newService() {
		return new Service(this);
	}

	public ComponentDescriptor descriptor(String id, boolean create) {
		var d = lookupOperation(RegistryService.lookUp.class).f(id);

		if (d == null && create) {
			lookupOperation(RegistryService.add.class).f(d = new ComponentDescriptor());
			d.name = id;
		}

		return d;
	}

	public ComponentDescriptor descriptor() {
		// outdates after 1 second
		if (descriptor == null || !descriptor.valid || descriptor.isOutOfDate()) {
			this.descriptor = createDescriptor();
		}

		return descriptor;
	}

	public ComponentDescriptor createDescriptor() {
		ComponentDescriptor d = new ComponentDescriptor();
		d.name = name;
		d.systemInfo = lookupOperation(SystemMonitor.get.class).f();
		forEachService(s -> d.services.add(s.descriptor()));
//		lookupService(NetworkingService.class).neighbors().forEach(n -> d.neighbors.add(n.friendlyName));

		for (TransportLayer protocol : lookup(NetworkingService.class).transport.transports()) {
			for (ComponentDescriptor peer : protocol.neighbors()) {
				var l = new ComponentDescriptor.Link();
				l.neighbor = peer.name;
				l.protocol = protocol.getName();
				d.links.add(l);
			}
		}

		return d;
	}

	@Override
	public String toString() {
		return descriptor.toString();
	}

	public static void stopPlatformThreads() {
		Service.threadPool.shutdown();
		LMI.executorService.shutdown();
	}

	public void removeService(Service s) {
		s.dispose();
		services.remove(s.id);
	}

	public To getAddress() {
		return new To(Set.of(descriptor));
	}

	public List<ComponentDescriptor> descriptors(String... names) {
		List<ComponentDescriptor> r = new ArrayList<>();

		for (var n : names) {
			r.add(descriptor(n, true));
		}
		return r;
	}

}
