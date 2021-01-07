package idawi;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import idawi.net.LMI;
import idawi.net.NetworkingService;
import idawi.routing.RoutingScheme_bcast;
import idawi.service.Bencher;
import idawi.service.ComponentDeployer;
import idawi.service.DummyService;
import idawi.service.ErrorLog;
import idawi.service.ExternalCommandsService;
import idawi.service.FileService;
import idawi.service.PingService;
import idawi.service.ServiceManager;
import idawi.service.rest.RESTService;
import toools.io.file.Directory;
import toools.util.Date;

public class Component {
	public static final Directory directory = new Directory("$HOME/" + Component.class.getPackage().getName());
	public static final ConcurrentHashMap<String, Component> componentsInThisJVM = new ConcurrentHashMap<>();

	private long id;
	private ComponentDescriptor descriptor;
	public String friendlyName;
	final Map<Class<? extends Service>, Service> services = new HashMap<>();
	public final Set<ComponentDescriptor> otherComponentsSharingFilesystem = new HashSet<>();
	public final Set<Component> killOnDeath = new HashSet<>();
	public ComponentDescriptor parent;

	public Component() {
		this("name=c" + componentsInThisJVM.size());
	}

	public Component(String cdl) {
		this(ComponentDescriptor.fromCDL(cdl));
	}

	public Component(ComponentDescriptor descriptor) {
		if (componentsInThisJVM.containsKey(descriptor.friendlyName)) {
			throw new IllegalStateException(descriptor.friendlyName + " is already in use in this JVM");
		}

		this.friendlyName = descriptor.friendlyName;
		this.id = descriptor.id;

		// start basic services
		new NetworkingService(this);
		new ServiceManager(this);
		new ComponentDeployer(this);
		new PingService(this);
		new Bencher(this);
//		new RoutingScheme1(this);
		new RoutingScheme_bcast(this);
		new ErrorLog(this);
		new DummyService(this);
		new RESTService(this);
		new ExternalCommandsService(this);
		new FileService(this);
		new RegistryService(this);

		this.descriptor = createDescriptor();

		// descriptorRegistry.add(descriptor());
		componentsInThisJVM.put(descriptor.friendlyName, this);
	}

	public void dispose() {
		killOnDeath.forEach(t -> t.dispose());
		componentsInThisJVM.remove(this);
	}

	public Collection<Service> services() {
		return services.values();
	}

	public <S> S lookupService(Class<S> id) {
		return (S) services.get(id);
	}

	public void lookupServices(Predicate<Service> p, Consumer<Service> h) {
		services().forEach(s -> {
			if (p.test(s)) {
				h.accept((Service) s);
			}
		});
	}

	public <S extends Service> void lookupServices(Class<S> c, Consumer<S> h) {
		lookupServices(s -> c.isInstance(s), s -> h.accept((S) s));
	}

	public Service newService() {
		return new Service(this);
	}

	public ComponentDescriptor descriptor() {
		// outdates after 1 second
		if (descriptor == null || Date.time() - descriptor.date > 1) {
			this.descriptor = createDescriptor();
		}

		return descriptor;
	}

	public ComponentDescriptor createDescriptor() {
		ComponentDescriptor d = new ComponentDescriptor();
		d.friendlyName = friendlyName;
		services().forEach(s -> d.services.add(s.descriptor()));
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
		s.shutdown();
		services.remove(s.id);
	}
}
