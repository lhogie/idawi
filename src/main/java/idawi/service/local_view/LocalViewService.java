package idawi.service.local_view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import idawi.Component;
import idawi.FunctionEndPoint;
import idawi.Idawi;
import idawi.ProcedureEndpoint;
import idawi.ProcedureNoInputEndpoint;
import idawi.SupplierEndPoint;
import idawi.messaging.Message;
import idawi.messaging.RoutingStrategy;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingService;
import idawi.routing.TrafficListener;
import idawi.service.DigitalTwinService;
import idawi.transport.Link;
import idawi.transport.TransportService;
import toools.io.Cout;
import toools.src.Source;

public class LocalViewService extends KnowledgeBase {

	public final List<DigitalTwinListener> listeners = new ArrayList<>();
	public final Network g = new Network();
	public final double disseminationIntervalS = 1;
	public int disseminationSampleSize = 50;
	public boolean disseminateTopologyChangesWhenTheyOccur = false;

	public LocalViewService(Component component) {
		super(component);

		// the component won't have a local twin of itself
		g.add(component);

		component.trafficListeners.add(new TrafficListener() {
			@Override
			public void newMessageReceived(TransportService t, Message m) {
				m.route.forEach(e -> e.link.markActive());
			}
		});

		// periodically disseminate topology information
		scheduleNextDisseminationMessage();

		// disseminate each local topology change as soon as they occur
		g.listeners.add(new NetworkTopologyListener() {

			@Override
			public void linkActivated(Link l) {
				// if this is a local link
				if (disseminateTopologyChangesWhenTheyOccur && l.asInfo().involves(component)) {
					exec(ComponentMatcher.all, LocalViewService.class, markLinkActive.class, msg -> {
						msg.content = List.of(l);
						msg.routingStrategy = new RoutingStrategy(routing());
					});
				}
			}

			@Override
			public void linkDeactivated(Link l) {
				// if this is a local link
				if (disseminateTopologyChangesWhenTheyOccur && l.asInfo().involves(component)) {
					exec(ComponentMatcher.all, LocalViewService.class, makeLinkInactive.class, msg -> {
						msg.routingStrategy = new RoutingStrategy(routing());

						msg.content = List.of(l);
					});
				}
			}

			@Override
			public void newComponent(Component p) {
			}

			@Override
			public void componentHasGone(Component a) {
			}

			@Override
			public void newLink(Link l) {
			}
		});
	}

	void scheduleNextDisseminationMessage() {
		if (disseminate()) {
			Idawi.agenda.schedule(new TopologyDisseminationEvent(Idawi.agenda.time() + disseminationIntervalS, this));
		}
	}

	public boolean disseminate() {
		return disseminationIntervalS > 0;
	}

	protected RoutingService<?> routing() {
		return component.need(BlindBroadcasting.class);
	}

	public List<Link> localLinks() {
		return g.findLinks(l -> l.src.component.equals(component));
	}

	@Override
	public String getFriendlyName() {
		return "digital twin service";
	}

	public class getComponentInfo extends FunctionEndPoint<Component, ComponentInfo> {
		public ComponentInfo f(Component c) {
			return c.service(DigitalTwinService.class).info();
		}

		@Override
		public String getDescription() {
			return "get the description for a particular component";
		}
	}

	public class acceptHello extends ProcedureEndpoint<Object> {
		@Override
		public void doIt(Object n) {
			Cout.debug(Source.here(), component + " received " + n);
//			Cout.debug("DT merge not yet implemented");
		}

		@Override
		public String getDescription() {
			return "feeds local DT with newly received data";
		}
	}

	public class components extends SupplierEndPoint<Collection<Component>> {
		@Override
		public Collection<Component> get() {
			return g.components();
		}

		@Override
		public String getDescription() {
			return "get all known components";
		}
	}

	public class clear extends ProcedureNoInputEndpoint {
		@Override
		public void doIt() {
			g.clear();
		}

		@Override
		public String getDescription() {
			return "clear the local view";
		}
	}

	@Override
	public Stream<Info> infos() {
		var infos = new ArrayList<Info>();
		for (var c : g.components()) {
			var dts = c.dt();

			if (dts != null)
				infos.add(dts.info());
		}
		return infos.stream();
	}

	public class makeLinkInactive extends ProcedureEndpoint<List<Link>> {
		@Override
		public void doIt(List<Link> list) {
			list.forEach(l -> g.deactivateLink(l));
		}

		@Override
		public String getDescription() {
			return "remove that link";
		}
	}

	public class markLinkActive extends ProcedureEndpoint<Iterable<Link>> {
		@Override
		public void doIt(Iterable<Link> list) {
			list.forEach(l -> l.markActive());
		}

		@Override
		public String getDescription() {
			return "mark that link active";
		}
	}

	public Object helloMessage() {
		return "hello from " + component;
	}

}
