package idawi.service.local_view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.routing.Route;
import idawi.routing.RouteListener;
import idawi.service.DigitalTwinService;
import idawi.service.digital_twin.info.KNW_Link;
import idawi.transport.Link;
import idawi.transport.OutLinks;

public class LocalViewService extends KnowledgeBase implements RouteListener {

	public final List<DigitalTwinListener> listeners = new ArrayList<>();

	public final Network g = new Network();

	public LocalViewService(Component component) {
		super(component);

		g.listeners.add(new NetworkTopologyListener() {

			@Override
			public void linkActivated(Link l) {
				if (l.involves(component)) {
					component.defaultRoutingProtocol().exec(LocalViewService.class, markLinkActive.class,
							new KNW_Link(now(), l));
				}
			}

			@Override
			public void linkDeactivated(Link l) {
				if (l.involves(component)) {
					component.defaultRoutingProtocol().exec(LocalViewService.class, makeLinkInactive.class,
							new KNW_Link(now(), l));
				}
			}

			@Override
			public void newComponent(Component p) {
			}

			@Override
			public void componentHasGone(Component a) {
			}
		});

		// the component won't have twin
//		components.add(component);
	}

	public Stream<Link> localLinks() {
		return g.links.stream().filter(l -> l.src.component.equals(component));
	}

	@Override
	public String getFriendlyName() {
		return "digital twin service";
	}

	public class getInfo extends TypedInnerClassEndpoint {
		public ComponentInfo f(Component c) {
			return localTwin(c).need(DigitalTwinService.class).info();
		}

		@Override
		public String getDescription() {
			return "get the description for a particular component";
		}
	}

	public class twinComponents extends TypedInnerClassEndpoint {
		public Set<Component> components() {
			return g.components;
		}

		@Override
		public String getDescription() {
			return "get all known components";
		}
	}

	public class clear extends TypedInnerClassEndpoint {
		public void f() {
			g.clear();
		}

		@Override
		public String getDescription() {
			return "clear the local view";
		}
	}

	@Override
	public Stream<Info> infos() {
		return g.components.stream().map(c -> c.need(DigitalTwinService.class)).map(s -> s.info());
	}

	@Override
	public void feedWith(Route route) {
		for (var l : route.entries()) {
			var tw = ensureTwinLinkExists(l.link);
			tw.activity.markActive();
		}
	}

	public OutLinks links() {
		return g.links;
	}

	public Link ensureTwinLinkExists(Link l) {
		localTwin(l.src);
		localTwin(l.dest);

		var twin = g.links.stream().filter(ll -> ll.equals(l)).findFirst().orElse(null);

		if (twin == null) {
			g.links.add(twin = l);
		} else {
			twin.latency = l.latency;
			twin.throughput = l.throughput;

			if (l.activity != null) {
				twin.activity.merge(l.activity);
			}

			twin.date = l.date;
		}

		return twin;
	}

	public Component localTwin(Component a) {
		Component twin = g.components.stream().filter(c -> c.equals(a)).findFirst().orElse(null);

		if (twin == null) {
			g.components.add(twin = new Component(a.name(), component));
			System.err.println(component + " creates new dt for " + a + " -> " + g.components);
		}

		var t = twin;
		listeners.forEach(l -> l.newComponent(t));
		return twin;
	}

	public <S extends Service> S localTwin(Class<S> s, Component child) {
		var twinComponent = localTwin(child);
		var twinService = twinComponent.need(s);

		if (twinService == null) {// the digital twin does have that service, let's add it
			twinService = twinComponent.need(s);
		}

		return twinService;
	}

	public <S extends Service> S localTwin(S s) {
		return localTwin((Class<S>) s.getClass(), s.component);
	}

	@Override
	public void accept(Info i) {
		if (i instanceof Link) {
			ensureTwinLinkExists((Link) i);
		} else if (i instanceof KNW_Link) {
			lostLink((KNW_Link) i);
		} else if (i instanceof ComponentInfo) {
			consider((ComponentInfo) i);
		} else {
			System.err.println("don't know what to do with info of class " + i.getClass().getName());
		}
	}

	private void consider(ComponentInfo d) {
		localTwin(d.component).dt().update(d);
	}

	public Collection<Component> components() {
		return g.components;
	}

	private void lostLink(KNW_Link linkOff) {
		g.links.set.removeIf(l -> l.src.equals(linkOff.from) && l.dest.equals(linkOff.to));
	}

	public class makeLinkInactive extends TypedInnerClassEndpoint {
		public void f(KNW_Link c) {
			g.links.set.removeIf(l -> l.matches(c.from, c.to));
		}

		@Override
		public String getDescription() {
			return "remove that link";
		}
	}

	public class markLinkActive extends TypedInnerClassEndpoint {
		public void f(KNW_Link c) {
			var l = g.findLink(c.from, c.to);

			if (l == null) {
				localTwin(c.from);
				localTwin(c.to);
				l = new Link(c.from, c.to);
				g.links.set.add(l);
			}

			l.activity.markActive();
		}

		@Override
		public String getDescription() {
			return "mark that link active";
		}
	}

}
