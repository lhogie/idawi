package idawi.service.local_view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Idawi;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.Message;
import idawi.routing.RoutingService;
import idawi.routing.TrafficListener;
import idawi.service.DigitalTwinService;
import idawi.transport.Link;
import idawi.transport.TransportService;
import toools.io.Cout;

public class LocalViewService extends KnowledgeBase {

	public final List<DigitalTwinListener> listeners = new ArrayList<>();
	public final Network g = new Network();
	public final double disseminationIntervalS = 0;
	public int disseminationSampleSize = 50;
	public boolean disseminateTopologyChangesWhenTheyOccur = false;

	public LocalViewService(Component component) {
		super(component);

		component.trafficListeners.add(new TrafficListener() {
			@Override
			public void newMessageReceived(TransportService t, Message m) {
				m.route.forEach(e -> g.markLinkActive(e.link));
			}
		});

		// periodically disseminate topology information
		scheduleNextDisseminationMessage();

		// disseminate each local topology change as soon as they occur
		g.listeners.add(new NetworkTopologyListener() {

			@Override
			public void linkActivated(Link l) {
				// if this is a local link
				if (disseminateTopologyChangesWhenTheyOccur && l.involves(component)) {
					routing().exec(LocalViewService.class, markLinkActive.class, List.of(l), true);
				}
			}

			@Override
			public void linkDeactivated(Link l) {
				// if this is a local link
				if (disseminateTopologyChangesWhenTheyOccur && l.involves(component)) {
					routing().exec(LocalViewService.class, makeLinkInactive.class, List.of(l), true);
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

	void scheduleNextDisseminationMessage() {
		if (disseminate()) {
			Idawi.agenda.schedule(new TopologyDisseminationEvent(Idawi.agenda.now() + disseminationIntervalS, this));
		}
	}

	public boolean disseminate() {
		return disseminationIntervalS > 0;
	}

	@Override
	protected RoutingService<?> routing() {
		return component.defaultRoutingProtocol();
	}

	public List<Link> localLinks() {
		return g.findLinks(l -> l.src.component.equals(component));
	}

	@Override
	public String getFriendlyName() {
		return "digital twin service";
	}

	public class getComponentInfo extends TypedInnerClassEndpoint {
		public ComponentInfo f(Component c) {
			return c.service(DigitalTwinService.class).info();
		}

		@Override
		public String getDescription() {
			return "get the description for a particular component";
		}
	}

	public class acceptHello extends TypedInnerClassEndpoint {
		public void f(Network n) {
			Cout.debug("DT merge not yet implemented");
		}

		@Override
		public String getDescription() {
			return "feeds local DT with newly received data";
		}
	}

	public class components extends TypedInnerClassEndpoint {
		public Collection<Component> f() {
			return g.components();
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
		return g.components().stream().map(c -> c.dt().info());
	}

	public class makeLinkInactive extends TypedInnerClassEndpoint {
		public void f(List<Link> list) {
			list.forEach(l -> g.deactivateLink(l));
		}

		@Override
		public String getDescription() {
			return "remove that link";
		}
	}

	public class markLinkActive extends TypedInnerClassEndpoint {
		public void f(Iterable<Link> list) {
			list.forEach(l -> g.markLinkActive(l));
		}

		@Override
		public String getDescription() {
			return "mark that link active";
		}
	}

	public Object helloMessage() {
		var bfs = g.bfs.get(component).get();
		return bfs.visitOrder.stream().filter(l -> bfs.distances.getLong(l.dest.component) < 2)
				.map(l -> l.dest.component).toList();
	}

}
