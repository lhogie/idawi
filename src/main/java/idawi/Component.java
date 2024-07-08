package idawi;

import java.lang.reflect.InvocationTargetException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import idawi.IdawiSerializer.ComponentRepresentative;
import idawi.routing.AutoForgettingLongList;
import idawi.routing.BlindBroadcasting;
import idawi.routing.RoutingService;
import idawi.routing.TrafficListener;
import idawi.routing.irp.IRP;
import idawi.service.DigitalTwinService;
import idawi.service.Location;
import idawi.service.LocationService;
import idawi.service.local_view.LocalViewService;
import idawi.transport.Link;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.text.NameList;

public class Component implements SizeOf {

	public final AutoForgettingLongList alreadySentMsgs = new AutoForgettingLongList(l -> l.size() < 1000);
	public final AutoForgettingLongList alreadyReceivedMsgs = new AutoForgettingLongList(l -> l.size() < 1000);

	final Set<Service> services = new HashSet<>();
	public boolean autonomous = false;
	public final List<TrafficListener> trafficListeners = new ArrayList<>();
	public String friendlyName = NameList.next();
	public KeyPair keyPair;
	public double birthDate = Idawi.agenda.time();

//	public final Set<CR> otherComponentsSharingFilesystem = new HashSet<>();
//	public final Set<Component> dependantChildren = new HashSet<>();
//	public Component deployer;
//	public Set<Component> simulatedComponents = new HashSet<>();

	public Component() {
//		new Error().printStackTrace();
	}

	public Component turnToDigitalTwin(Component host) {
		var s = new DigitalTwinService(this);
		s.host = host.localView();
		return this;
	}

	public Component twin(boolean includeServices) {
		var twin = new Component();

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

	public <S extends Service> S need(Class<S> c) {
		return service(c, true);
	}

	public List<? extends Service> need(Class<? extends Service>... c) {
		return Arrays.stream(c).map(cc -> need(cc)).toList();
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
		var s = friendlyName;

		if (s == null) {
			var pk = publicKey();

			if (pk == null) {
				s = "*unnamed*";
			} else {
				s = new String(Base64.getEncoder().encode(pk.getEncoded()));
			}
		}

		return isDigitalTwin() ? s + "*" : s;
	}

	public double now() {
		return Idawi.agenda.time();
		// var ts = lookup(TimeService.class);
		// return ts == null ? Date.time() : ts.now();
	}

	public LocalViewService localView() {
		var dt = dt();
		return dt == null ? need(LocalViewService.class) : dt.host;
	}

	public RoutingService<?> defaultRoutingProtocol() {
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
		sum += 8;
		sum += SizeOf.sizeOf(friendlyName);
		sum += 8;

		for (var s : services) {
			sum += 8 + s.sizeOf();
		}

		return sum + alreadySentMsgs.sizeOf() + alreadyReceivedMsgs.sizeOf() + 8;
	}

	public List<Link> outLinks() {
		return localView().g.findLinks(l -> l.src.component.equals(this));
	}

	public PublicKey publicKey() {
		return keyPair == null ? null : keyPair.getPublic();
	}

	@Override
	public boolean equals(Object o) {
		return hashCode() == ((Component) o).hashCode();
	}

	@Override
	public int hashCode() {
		if (publicKey() != null) {
			return publicKey().hashCode();
		} else if (friendlyName != null) {
			return friendlyName.hashCode();
		} else {
			return Double.hashCode(birthDate);
		}
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

	public ComponentRepresentative representative() {
		var cr = new ComponentRepresentative();
		cr.key = publicKey();
		cr.friendlyName = friendlyName;
		return cr;
	}

}
