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
import idawi.routing.RoutingService;
import idawi.service.Bencher;
import idawi.service.ComponentDeployer;
import idawi.service.DummyService;
import idawi.service.ExternalCommandsService;
import idawi.service.PingPong;
import idawi.service.RESTService;
import idawi.service.ServiceManager;
import toools.io.file.Directory;
import toools.reflect.Clazz;

public class Component {
	public static final Directory directory = new Directory("$HOME/." + Component.class.getPackage().getName());
	public static final ConcurrentHashMap<String, Component> thingsInThisJVM = new ConcurrentHashMap<>();
	public static PeerRegistry descriptorRegistry = new PeerRegistry(new Directory(directory, "registry"));

	private ComponentInfo descriptor;
	 final Map<Class<? extends Service>, Service> services = new HashMap<>();
	public Set<ComponentInfo> otherComponentsSharingFilesystem;
	public final Set<Component> killOnDeath = new HashSet<>();

	public Component() {
		this("name=c" + thingsInThisJVM.size());
	}

	public Component(String cdl) {
		this(ComponentInfo.fromPDL(cdl));
	}

	public Component(ComponentInfo descriptor) {
		if (thingsInThisJVM.containsKey(descriptor.friendlyName)) {
			throw new IllegalStateException(descriptor.friendlyName + " is already in use");
		}

		this.descriptor = descriptor;

		// start basic services
		addService(NetworkingService.class);
		addService(ServiceManager.class);
		addService(ComponentDeployer.class);
		addService(PingPong.class);
		addService(Bencher.class);
		addService(RoutingService.class);
//		addService(ErrorLog.class);
		addService(DummyService.class);
		addService(RESTService.class);
		addService(ExternalCommandsService.class);

		descriptorRegistry.add(descriptor());
		thingsInThisJVM.put(descriptor.friendlyName, this);
	}

	public void dispose() {
		killOnDeath.forEach(t -> t.dispose());
		thingsInThisJVM.remove(this);
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

	public <S> S addService(Class<S> id) {
		if (lookupService(id) != null) {
			throw new IllegalArgumentException("service already running");
		}

		return Clazz.makeInstance(Clazz.getConstructor(id, Component.class), this);
	}

	public Service newService() {
		return new Service(this);
	}

	public ComponentInfo descriptor() {
		return descriptor;
	}

	public void updateDescriptor() {
		for (var s : services()) {
			s.inform(descriptor);
		}
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
