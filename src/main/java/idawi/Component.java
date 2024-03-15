package idawi;

import java.lang.reflect.InvocationTargetException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import idawi.routing.AutoForgettingLongList;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ForceBroadcasting;
import idawi.routing.RoutingService;
import idawi.routing.TrafficListener;
import idawi.routing.irp.IRP;
import idawi.service.DigitalTwinService;
import idawi.service.Location;
import idawi.service.LocationService;
import idawi.service.ServiceManager;
import idawi.service.local_view.LocalViewService;
import idawi.service.web.WebService;
import idawi.transport.Link;
import idawi.transport.SharedMemoryTransport;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.security.SecureSerializer;

public class Component implements SizeOf {

	public final AutoForgettingLongList alreadyKnownMsgs = new AutoForgettingLongList(l -> l.size() < 1000);

	// used to serialized messages for transport
	public final transient SecureSerializer secureSerializer;

	final Set<Service> services = new HashSet<>();
	public boolean autonomous = false;
	public final List<TrafficListener> trafficListeners = new ArrayList<>();
	public String friendlyName;

//	public final Set<CR> otherComponentsSharingFilesystem = new HashSet<>();
//	public final Set<Component> dependantChildren = new HashSet<>();
//	public Component deployer;
//	public Set<Component> simulatedComponents = new HashSet<>();

	
	public Component() {
		secureSerializer = new SecureSerializer(new IdawiSerializer(this), Idawi.enableEncryption);
	}

	public Component(PublicKey k) {
		secureSerializer = new SecureSerializer(k, new IdawiSerializer(this));
	}

	public Component turnToDigitalTwin(Component host) {
		var s = new DigitalTwinService(this);
		s.host = host.localView();
		return this;
	}

	public Component twin(boolean includeServices) {
		var twin = new Component(secureSerializer.publicKey());

		if (includeServices) {
			for (var s : services) {
				try {
					s.getClass().getConstructor(Component.class).newInstance(twin);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}

		return twin;
	}

	public void addBasicServices() {
		new LocalViewService(this);
		new WebService(this);
		new SharedMemoryTransport(this);
		new BlindBroadcasting(this);
		new IRP(this);
		new ServiceManager(this);
		new ForceBroadcasting(this);
	}

	public boolean isDigitalTwin() {
		return has(DigitalTwinService.class);
	}

	public <S extends Service> S start(Class<S> serviceID) {
		if (!services(serviceID).isEmpty())
			throw new IllegalArgumentException("service already running");

		try {
			return serviceID.getConstructor(Component.class).newInstance(this);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	public void dispose() {
		// componentsInThisJVM.remove(this);
		services.forEach(s -> s.dispose());
	}

	public Collection<Service> services() {
		return services;
	}

	public void forEachService(Consumer<Service> c) {
		services.forEach(s -> c.accept(s));
	}

	public <S extends Service> List<S> services(Class<S> id) {
		return (List<S>) services.stream().filter(s -> id.isAssignableFrom(s.getClass())).toList();
	}

	public <S extends Service> List<S> services(Predicate<Service> p) {
		return (List<S>) services.stream().filter(p).toList();
	}

	public <S extends Service> S service(Class<S> c) {
		return service(c, false);
	}

	public <S extends Service> S service(Class<S> c, boolean autoload) {
		for (var s : services) {
			if (c.isAssignableFrom(s.getClass())) {
				return (S) s;
			}
		}

		if (autoload) {
			// Cout.debug("starting " + c + " on " + this);
			return start(c);
		} else {
			return null;
		}
	}

	public boolean has(Class<? extends Service> c) {
		return service(c, false) != null;
	}

	public void forEachService(Predicate<Service> predicate, Consumer<Service> h) {
		forEachService(service -> {
			if (predicate.test(service)) {
				h.accept((Service) service);
			}
		});
	}

	public <S> void forEachService(Class<S> serviceID, Consumer<S> h) {
		forEachService(s -> serviceID.isInstance(s), s -> h.accept((S) s));
	}

	@Override
	public String toString() {
		var name = friendlyName;

		if (name == null) {
			var pk = publicKey();

			if (pk == null) {
				name = "*unnamed*";
			} else {
				name = new String(Base64.getEncoder().encode(pk.getEncoded()));
			}
		}

		return isDigitalTwin() ? dt().host.component + "/" + name : name;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public double now() {
		return Idawi.agenda.now();
		// var ts = lookup(TimeService.class);
		// return ts == null ? Date.time() : ts.now();
	}

	public LocalViewService localView() {
		var dt = dt();
		return dt == null ? service(LocalViewService.class, true) : dt.host;
	}

	public RoutingService defaultRoutingProtocol() {
		return bb();
	}

	public BlindBroadcasting bb() {
		return service(BlindBroadcasting.class, true);
	}

	public IRP irp() {
		return service(IRP.class, true);
	}

	public Location getLocation() {
		var locationService = service(LocationService.class);
		return locationService == null ? null : locationService.location;
	}

	@Override
	public long sizeOf() {
		long sum = 8; // dts
		sum += secureSerializer.sizeOf();
		sum += 8;
		sum += SizeOf.sizeOf(friendlyName);
		sum += 8;

		for (var s : services) {
			sum += 8 + s.sizeOf();
		}

		return sum + alreadyKnownMsgs.sizeOf() + 8;
	}

	public Long longHash() {
		long h = 1125899906842597L;
		String s = new String(publicKey().getEncoded());
		int len = s.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + s.charAt(i);
		}

		return h;
	}

	public List<Link> outLinks() {
		return localView().g.findLinks(l -> l.src.component.equals(this));
	}

	public PublicKey publicKey() {
		return secureSerializer.publicKey();
	}

	@Override
	public boolean equals(Object o) {
		var c = (Component) o;
		return c.publicKey().equals(publicKey());
	}

	public DigitalTwinService dt() {
		return service(DigitalTwinService.class);
	}

	public static List<Component> createNComponent(int n) {
		var components = new ArrayList<Component>();

		for (int i = 0; i < n; ++i) {
			components.add(new Component());
		}

		return components;
	}

	public static final Directory directory = new Directory("$HOME/" + Component.class.getPackage().getName());
	public static final ConcurrentHashMap<String, Component> componentsInThisJVM = new ConcurrentHashMap<>();

}
