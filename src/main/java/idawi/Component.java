package idawi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.crypto.SecretKey;

import idawi.deploy.DeployerService;
import idawi.knowledge_base.ComponentDescription;
import idawi.knowledge_base.ComponentRef;
import idawi.knowledge_base.MapService;
import idawi.knowledge_base.MiscKnowledgeBase;
import idawi.routing.BlindBroadcasting;
import idawi.routing.FloodingWithSelfPruning;
import idawi.routing.FloodingWithSelfPruning_UsingBloomFilter;
import idawi.routing.RoutingService;
import idawi.routing.irp.IRP;
import idawi.service.Bencher;
import idawi.service.DemoService;
import idawi.service.DirectorySharingService;
import idawi.service.ErrorLog;
import idawi.service.LocationService2;
import idawi.service.LocationService2.Location;
import idawi.service.ServiceManager;
import idawi.service.SystemMonitor;
import idawi.service.extern.ExternalCommandsService;
import idawi.service.rest.AESEncrypter;
import idawi.service.rest.WebService;
import idawi.service.time.TimeService;
import idawi.transport.PipeFromToChildProcess;
import toools.io.file.Directory;
import toools.util.Date;

public class Component {
	public static final Directory directory = new Directory("$HOME/" + Component.class.getPackage().getName());
	public static final ConcurrentHashMap<ComponentRef, Component> componentsInThisJVM = new ConcurrentHashMap<>();

	static AESEncrypter aes = new AESEncrypter();
	SecretKey key = null;

	final Set<Service> services = new HashSet<>();
//	public final Set<CR> otherComponentsSharingFilesystem = new HashSet<>();
	public final Set<Component> killOnDeath = new HashSet<>();
	public ComponentRef parent;
	private ComponentRef ref;

	public Component() {
		this(new ComponentRef("c" + componentsInThisJVM.size()));
	}

	public Component(ComponentRef ref) {
		if (componentsInThisJVM.containsKey(ref)) {
			throw new IllegalStateException(ref + " is already in use in this JVM");
		}

		this.ref = ref;
		this.ref.component = this;

		new MiscKnowledgeBase(this);
		new MapService(this);
		new TimeService(this);

		// start network services
//		new SharedMemoryTransport(this, "shared memory");
//		new TCPDriver(this);
//		new UDPDriver(this);
		new PipeFromToChildProcess(this);

		// routing
		new BlindBroadcasting(this);
		new FloodingWithSelfPruning(this);
		new FloodingWithSelfPruning_UsingBloomFilter(this);
		new IRP(this);

		// start system services
		new DeployerService(this);
		new ServiceManager(this);
		new SystemMonitor(this);
		new Bencher(this);
		new ErrorLog(this);
		new WebService(this);
		new ExternalCommandsService(this);
		new DirectorySharingService(this);
		new DemoService(this);

		// descriptorRegistry.add(descriptor());
		componentsInThisJVM.put(ref, this);
	}

	public void dispose() {
		componentsInThisJVM.remove(this);
		services.forEach(s -> s.dispose());
		killOnDeath.forEach(t -> t.dispose());
	}

	public Collection<Service> services() {
		return services;
	}

	public void forEachService(Consumer<Service> c) {
		services.forEach(s -> c.accept(s));
	}

	public <S extends Service> List<S> services(Class<S> id) {
		List<S> l = new ArrayList<>();

		for (var s : services) {
			if (id.isAssignableFrom(s.getClass())) {
				l.add((S) s);
			}
		}

		return l;
	}

	public <S extends Service> S lookup(Class<S> id) {
		for (var s : services) {
			if (id.isAssignableFrom(s.getClass())) {
				return (S) s;
			}
		}

		return null;
	}

	public <O extends InnerClassOperation> O operation(Class<O> id) {
		var serviceClass = InnerClassOperation.serviceClass(id);
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

	public <S extends InnerClassOperation> S lookupOperation(Class<? extends S> c) {
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

	@Override
	public String toString() {
		return ref().toString();
	}

	public static void stopPlatformThreads() {
		Service.threadPool.shutdown();
	}

	public void removeService(Service s) {
		s.dispose();
		services.remove(s.id);
	}

	public ComponentDescription descriptor() {
		return lookup(MiscKnowledgeBase.class).componentDescriptor();
	}

	public ComponentRef ref() {
		return ref;
	}

	public double now() {
		var ts = lookup(TimeService.class);
		return ts == null ? Date.time() : ts.now();
	}

	public MapService mapService() {
		return lookup(MapService.class);
	}

	public MiscKnowledgeBase knowledgeBase() {
		return lookup(MiscKnowledgeBase.class);
	}

	public RoutingService defaultRoutingProtocol() {
		return lookup(BlindBroadcasting.class);
	}

	public FloodingWithSelfPruning fwsp() {
		return lookup(FloodingWithSelfPruning.class);
	}

	public BlindBroadcasting bb() {
		return lookup(BlindBroadcasting.class);
	}

	public IRP irp() {
		return lookup(IRP.class);
	}

	public Location getLocation() {
		var locationService = lookup(LocationService2.class);
		return locationService == null ? null : locationService.location;
	}
}
