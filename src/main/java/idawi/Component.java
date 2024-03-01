package idawi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import idawi.routing.BlindBroadcasting;
import idawi.routing.ForceBroadcasting;
import idawi.routing.RoutingService;
import idawi.routing.TrafficListener;
import idawi.routing.irp.IRP;
import idawi.security.AES;
import idawi.security.RSA;
import idawi.service.DigitalTwinService;
import idawi.service.Location;
import idawi.service.LocationService;
import idawi.service.ServiceManager;
import idawi.service.local_view.LocalViewService;
import idawi.service.web.WebService;
import idawi.transport.Link;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.TransportService;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;

public class Component implements SizeOf {

	// used to serialized messages for transport
	public transient Serializer serializer = new Serializer() {
		static class E implements Serializable {
			PublicKey publicKey;
			byte[] encryptedAESKey;
			byte[] data;
		}

		@Override
		public Object read(InputStream is) throws IOException {
			E e = (E) javaSerializer.read(is);

			if (e.encryptedAESKey == null) {
				return javaSerializer.fromBytes(e.data);
			} else {
				var plainAESKey = rsa.decode(e.publicKey, e.encryptedAESKey);
				Key aesKey = (Key) javaSerializer.fromBytes(plainAESKey);
				var plainMsg = AES.decode(e.data, aesKey);
				return javaSerializer.fromBytes(plainMsg);
			}
		}

		@Override
		public void write(Object o, OutputStream os) throws IOException {
			E e = new E();
			e.publicKey = rsa.keyPair.getPublic();

			if (rsa.keyPair.getPrivate() == null) {
				e.data = javaSerializer.toBytes(o);
			} else {
				var aesKey = AES.getRandomKey(128);
				e.encryptedAESKey = rsa.encode(javaSerializer.toBytes(aesKey));
				e.data = AES.encode(javaSerializer.toBytes(o), aesKey);
			}

			javaSerializer.write(e, os);
		}

		@Override
		public String getMIMEType() {
			return "idawi";
		}

		@Override
		public boolean isBinary() {
			return true;
		}

		private JavaSerializer javaSerializer = new JavaSerializer() {

			static class ComponentRepresentative implements Serializable {
				PublicKey key;
			}

			static class LinkRepresentative implements Serializable {
				Component srcC;
				Class<? extends TransportService> srcT;
				Component destC;
				Class<? extends TransportService> destT;
			}

			@Override
			protected Object replaceAtDeserialization(Object o) {
				if (o instanceof ComponentRepresentative) {
					var key = ((ComponentRepresentative) o).key;
					var alreadyIn = localView().g.findComponentByPublicKey(key);
					return alreadyIn != null ? alreadyIn : new Component(key).turnToDigitalTwin(Component.this);
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
					cr.key = ((Component) o).id();
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
	};

	public static List<Component> createNComponent(int n) {
		var components = new ArrayList<Component>();

		for (int i = 0; i < n; ++i) {
			components.add(new Component());
		}

		return components;
	}

	public static final Directory directory = new Directory("$HOME/" + Component.class.getPackage().getName());
	public static final ConcurrentHashMap<String, Component> componentsInThisJVM = new ConcurrentHashMap<>();

	RSA rsa = new RSA();
	final Set<Service> services = new HashSet<>();
//	public final Set<CR> otherComponentsSharingFilesystem = new HashSet<>();
	public final Set<Component> dependantChildren = new HashSet<>();
	public Component deployer;
	public boolean autonomous = false;
	public final List<TrafficListener> trafficListeners = new ArrayList<>();
	public Set<Component> simulatedComponents = new HashSet<>();
	public String friendlyName;

	public Component() {
		rsa.random(Idawi.enableEncryption);
	}

	public Component(PublicKey k) {
		rsa.keyPair = new KeyPair(k, null);
	}

	public Component turnToDigitalTwin(Component host) {
		var s = new DigitalTwinService(this);
		s.host = host.localView();
		return this;
	}

	public Component twin(boolean includeServices) {
		var twin = new Component(rsa.keyPair.getPublic());

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
			if (id() == null) {
				name = "*unnamed*";
			} else {
				name = new String(id().getEncoded());
			}
		}

		return isDigitalTwin() ? dt().host.component + "/" + name : name;
	}

	@Override
	public int hashCode() {
		return id().hashCode();
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
		sum += rsa.keyPair.getPublic().getEncoded().length + rsa.keyPair.getPrivate().getEncoded().length;
		sum += 8;
		sum += SizeOf.sizeOf(id());
		sum += 8;

		for (var s : services) {
			sum += 8 + s.sizeOf();
		}

		return sum;
	}

	public Long longHash() {
		long h = 1125899906842597L;
		String s = new String(id().getEncoded());
		int len = s.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + s.charAt(i);
		}

		return h;
	}

	public List<Link> outLinks() {
		return localView().g.findLinks(l -> l.src.component.equals(this));
	}

	@Override
	public boolean equals(Object o) {
		var c = (Component) o;
		return c.id().equals(id());
	}

	public DigitalTwinService dt() {
		return service(DigitalTwinService.class);
	}

	public PublicKey id() {
		return rsa.keyPair.getPublic();
	}

	public TransportService alreadyReceivedMsg(long ID) {
		for (var t : services(TransportService.class)) {
			if (t.alreadyKnownMsgs.contains(ID)) {
				return t;
			}
		}

		return null;
	}

	public String friendlyName() {
		return friendlyName;
	}

}
