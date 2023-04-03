package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.crypto.SecretKey;

import idawi.knowledge_base.ComponentInfo;
import idawi.knowledge_base.DigitalTwinService;
import idawi.knowledge_base.MiscKnowledgeBase;
import idawi.routing.BlindBroadcasting;
import idawi.routing.FloodingWithSelfPruning;
import idawi.routing.RoutingService;
import idawi.routing.irp.IRP;
import idawi.service.Location;
import idawi.service.LocationService;
import idawi.service.time.TimeService;
import idawi.service.web.AESEncrypter;
import idawi.transport.Neighborhood;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.TransportService;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.util.Date;

public class Component implements SizeOf, Externalizable {
	public static final Directory directory = new Directory("$HOME/" + Component.class.getPackage().getName());
	public static final ConcurrentHashMap<String, Component> componentsInThisJVM = new ConcurrentHashMap<>();

	static AESEncrypter aes = new AESEncrypter();

	public static List<Component> create(String prefix, int n) {
		var r = new ArrayList<Component>();

		for (int i = 0; i < n; ++i) {
			r.add(new Component(prefix + i));
		}

		return r;
	}

	public transient ComponentInfo info;

	transient SecretKey key = null;

	transient final Set<Service> services = new HashSet<>();
//	public final Set<CR> otherComponentsSharingFilesystem = new HashSet<>();
	transient public final Set<Component> dependantChildren = new HashSet<>();
	transient public Component parent;
	public String ref;

	public Component() {
		this("c" + componentsInThisJVM.size());
	}

	public Component(String ref) {
		if (componentsInThisJVM.containsKey(ref)) {
			throw new IllegalStateException(ref + " is already in use in this JVM");
		}

		this.ref = ref;

		new SharedMemoryTransport(this);
		new DigitalTwinService(this);
		new BlindBroadcasting(this);

		// descriptorRegistry.add(descriptor());
		componentsInThisJVM.put(ref, this);
	}

	public void dispose() {
		componentsInThisJVM.remove(this);
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

	@Override
	public String toString() {
		return ref.toString();
	}

	@Override
	public int hashCode() {
		return ref.hashCode();
	}

	public static void stopPlatformThreads() {
		Service.threadPool.shutdown();
	}

	public void removeService(Service s) {
		s.dispose();
		services.remove(s.id);
	}

	public ComponentInfo descriptor() {
		return lookup(MiscKnowledgeBase.class).componentInfo();
	}

	public double now() {
		var ts = lookup(TimeService.class);
		return ts == null ? Date.time() : ts.now();
	}

	public DigitalTwinService digitalTwinService() {
		return lookup(DigitalTwinService.class);
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
		var locationService = lookup(LocationService.class);
		return locationService == null ? null : locationService.location;
	}

	@Override
	public long sizeOf() {
		long sum = 8; // dts
		sum += key == null ? 0 : key.getEncoded().length;
		sum += 8;
		sum += SizeOf.sizeOf(ref);
		sum += 8;

		for (var s : services) {
			sum += 8 + s.sizeOf();
		}

		return sum;
	}

	public Long longHash() {
		long h = 1125899906842597L;
		int len = ref.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + ref.charAt(i);
		}

		return h;
	}

	public int nbNeighbors() {
		return services(TransportService.class).stream().map(s -> s.neighborhood().size()).reduce((t, u) -> t + u)
				.get();
	}

	public Neighborhood neighbors() {
		return Neighborhood.merge(services(TransportService.class).stream().map(t -> t.neighborhood()).toList()
				.toArray(new Neighborhood[0]));
	}

	public Collection<TransportService> ins() {
		var r = new HashSet<TransportService>();

		for (var c : lookup(DigitalTwinService.class).components) {
			for (var t : c.services(TransportService.class)) {
				for (var n : t.neighborhood().infos()) {
					if (n.dest.equals(this)) {
						r.add(t);
					}
				}
			}
		}

		return r;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(ref);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		ref = in.readUTF();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Component && o.hashCode() == hashCode();
	}
}
