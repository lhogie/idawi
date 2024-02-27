package idawi;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.crypto.SecretKey;

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
import idawi.service.web.AESEncrypter;
import idawi.service.web.WebService;
import idawi.transport.Link;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.TransportService;
import toools.SizeOf;
import toools.io.Cout;
import toools.io.file.Directory;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;

public class Component implements SizeOf {

	static class ComponentRepresentative implements Serializable {
		String name;
	}

	static class LinkRepresentative implements Serializable {
		Component srcC;
		Class<? extends TransportService> srcT;
		Component destC;
		Class<? extends TransportService> destT;
	}

	// used to serialized messages for transport
	public Serializer serializer = new JavaSerializer() {

		@Override
		protected Object replaceAtDeserialization(Object o) {
			if (o instanceof ComponentRepresentative) {
				var alreadyIn = localView().g.findComponentByName(((ComponentRepresentative) o).name);
				return alreadyIn != null ? alreadyIn : new Component(name, Component.this);
			} else if (o instanceof LinkRepresentative) {
				var r = (LinkRepresentative) o;
				var src = r.srcC.service(r.srcT, true);
				var dest = r.destC.service(r.destT, true);
				var l = localView().g.findALinkConnecting(src, dest);
				return l != null ? l : new Link(src, dest);
			} else {
				return o;
			}
		}

		@Override
		protected Object replaceAtSerialization(Object o) {
			if (o instanceof Component) {
				var cr = new ComponentRepresentative();
				cr.name = ((Component) o).name;
				return cr;
			} else if (o instanceof Link) {
				var l = (Link) o;
				var r = new LinkRepresentative();
				r.srcC = l.src.component;
				r.srcT = l.src.getClass();
				r.destC = l.dest.component;
				r.destT = l.dest.getClass();
				return r;
			} else {
				return super.replaceAtSerialization(o);
			}
		}
	};

	public static List<Component> createNComponent(String prefix, int n) {
		var components = new ArrayList<Component>();

		for (int i = 0; i < n; ++i) {
			components.add(new Component(prefix + i, true));
		}

		return components;
	}

	public static final Directory directory = new Directory("$HOME/" + Component.class.getPackage().getName());
	public static final ConcurrentHashMap<String, Component> componentsInThisJVM = new ConcurrentHashMap<>();

	static AESEncrypter aes = new AESEncrypter();

	SecretKey key = null;
	final Set<Service> services = new HashSet<>();
//	public final Set<CR> otherComponentsSharingFilesystem = new HashSet<>();
	public final Set<Component> dependantChildren = new HashSet<>();
	public Component deployer;
	private String name;
	public boolean autonomous = false;
	public final List<TrafficListener> trafficListeners = new ArrayList<>();
	public Set<Component> simulatedComponents = new HashSet<>();

	public Component() {
		this(Long.toHexString(new Random().nextLong()), false);
	}

	public Component(String name) {
		Objects.requireNonNull(name);
		this.name = name;
	}

	public Component(String name, boolean installBaseServices) {
		this(name);

		if (installBaseServices) {
			addBasicServices();
		}

		// descriptorRegistry.add(descriptor());
//		componentsInThisJVM.put(ref, this);
	}

	public Component twin(boolean includeServices) {
		var twin = new Component(name);
		new DigitalTwinService(twin, localView());

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

	public Component(String name, LocalViewService host) {
		this(name, false);
		new DigitalTwinService(this, host);
	}

	// makes a digital twin
	public Component(String name, Component realComponent) {
		this(name, realComponent.localView());
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

	public Component createDigitalTwinFor(String id) {
		return new Component(id, localView());
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
		dependantChildren.forEach(t -> t.dispose());
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
		//	Cout.debug("starting " + c + " on " + this);
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
		var n = name == null ? "unnamed" : name.toString();
		return isDigitalTwin() ? dt().host.component + "/" + n : n;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
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
		sum += key == null ? 0 : key.getEncoded().length;
		sum += 8;
		sum += SizeOf.sizeOf(name);
		sum += 8;

		for (var s : services) {
			sum += 8 + s.sizeOf();
		}

		return sum;
	}

	public Long longHash() {
		long h = 1125899906842597L;
		int len = name.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + name.charAt(i);
		}

		return h;
	}

	public List<Link> outLinks() {
		return localView().g.findLinks(l -> l.src.component.equals(this));
	}

	@Override
	public boolean equals(Object o) {
		var c = (Component) o;
		return c.name().equals(name);
	}

	public DigitalTwinService dt() {
		return service(DigitalTwinService.class);
	}

	public String name() {
		return name;
	}
	
	public TransportService alreadyReceivedMsg(long ID) {
		for (var t : services(TransportService.class)){
			if (t.alreadyKnownMsgs.contains(ID)) {
				return t;
			}
		}
		
		return null;
	}


}
