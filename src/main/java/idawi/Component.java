package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.crypto.SecretKey;

import idawi.routing.BlindBroadcasting;
import idawi.routing.ForceBroadcasting;
import idawi.routing.RoutingService;
import idawi.routing.irp.IRP;
import idawi.service.DigitalTwinService;
import idawi.service.Location;
import idawi.service.LocationService;
import idawi.service.ServiceManager;
import idawi.service.local_view.LocalViewService;
import idawi.service.time.TimeService;
import idawi.service.web.AESEncrypter;
import idawi.service.web.WebService;
import idawi.transport.Link;
import idawi.transport.OutLinks;
import idawi.transport.SharedMemoryTransport;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.util.Date;

public class Component implements SizeOf, Externalizable {
	public static List<Component> createNComponent(String prefix, int n) {
		var r = new ArrayList<Component>();

		for (int i = 0; i < n; ++i) {
			r.add(new Component(prefix + i, true));
		}

		return r;
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

	public Component() {
		this(Long.toHexString(new Random().nextLong()), false);
	}

	public Component(String name) {
		if (name == null)
			throw new NullPointerException();

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

	public Component(String name, LocalViewService host) {
		this(name, false);
		new DigitalTwinService(this, host);
	}

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

	public <S extends Service> S need(Class<S> id) {
		var l = services(id);

		if (l.isEmpty()) {
			return start(id);
		} else {
			return l.get(0);
		}
	}
	
	public boolean has(Class<? extends Service> c) {
		return services.stream().anyMatch(s -> c.isAssignableFrom(s.getClass()));
	}

	public <S extends Service> S lookup(Class<S> c) {
		return (S) services.stream().filter(s -> c.isAssignableFrom(s.getClass())).findFirst().orElse(null);
	}

	public Endpoint lookup(Class<? extends Service> service, Class<? extends InnerClassEndpoint> id) {
		return need(service).lookupEndpoint(id.getSimpleName());
	}

	public void forEachService(Predicate<Service> predicate, Consumer<Service> h) {
		forEachService(service -> {
			if (predicate.test(service)) {
				h.accept((Service) service);
			}
		});
	}

	public <S> void forEachServiceOfClass(Class<S> serviceID, Consumer<S> h) {
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
		return Service.now();
		//var ts = lookup(TimeService.class);
		//return ts == null ? Date.time() : ts.now();
	}

	public LocalViewService localView() {
		var dt = dt();
		return dt == null ? need(LocalViewService.class) : dt.host;
	}

	public RoutingService defaultRoutingProtocol() {
		return need(BlindBroadcasting.class);
	}

	public BlindBroadcasting bb() {
		return need(BlindBroadcasting.class);
	}

	public IRP irp() {
		return need(IRP.class);
	}

	public Location getLocation() {
		var locationService = need(LocationService.class);
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

	public OutLinks outLinks() {
		return new OutLinks(localView().links().stream().filter(l -> l.src.component.equals(this)).toList());
	}

	public List<Link> ins() {
		return localView().links().stream().filter(l -> l.dest.component.equals(this)).toList();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(name);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		name = in.readUTF();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Component && o.hashCode() == hashCode();
	}

	public boolean matches(String re) {
		return name.matches(re);
	}

	public DigitalTwinService dt() {
		return lookup(DigitalTwinService.class);
	}

	public String name() {
		return name;
	}


}
