package idawi.demo.valentin;

import java.util.List;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.routing.TrafficListener;
import idawi.service.LocationService;
import idawi.service.local_view.LocalViewService;
import idawi.service.local_view.Network;
import idawi.transport.TCPDriver;
import idawi.transport.Topologies;
import idawi.transport.TransportService;
import idawi.transport.WiFiDirect;
import jdotgen.GraphvizDriver;
import jdotgen.GraphvizDriver.DOTCFG;
import jdotgen.Props.Style;
import jexperiment.AVGMODE;
import jexperiment.GNUPlot;
import toools.collections.Collections;

public class Valentin {

	static class CloudEdgeFog {
		private List<Component> components;
		private List<Component> mobileDevices;
		private List<Component> hotspots;

		public CloudEdgeFog(int n) {
			components = Component.createNComponent(n);
			System.out.println("assign random locations to nodes");
			components.forEach(c -> c.service(LocationService.class, true).location.random(1000, Idawi.prng));

			// build individual local views
			for (var a : components) {
				var lv = a.service(LocalViewService.class, true);

				for (var b : components) {
					var btwin = lv.g.ensureExists(b);
					btwin.service(LocationService.class, true).location = b.getLocation().clone();
				}
			}

			System.out.println("generates a random topology involving/informing all components");

			mobileDevices = components.subList(3, components.size());
			hotspots = components.subList(0, 3);

			// link mobile devices together
			Topologies.wirelessMesh(mobileDevices, (a, b) -> WiFiDirect.class, components);

			// link hotspots together
			Topologies.chain(hotspots, (a, b) -> TCPDriver.class, components);

			// link 1/4 of mobile devices to their closest hotspot
			var mobileDevicesConnectedToHotspots = Collections.pickRandomSubset(mobileDevices, mobileDevices.size() / 4,
					false, Idawi.prng);
			mobileDevicesConnectedToHotspots.forEach(mobileDevice -> {
				var closestHotspot = Topologies.sortByDistanceTo(hotspots, mobileDevice).get(0);
				Network.markLinkActive(closestHotspot, mobileDevice, TCPDriver.class, true, components);
			});
		}

		public void plot(Component c0) {
			c0.localView().g.plot("test", customizer -> {
				customizer.showLink = l -> !l.isLoop();
				customizer.linkStyle = l -> l.src.getClass() == WiFiDirect.class ? Style.dotted : Style.solid;
				customizer.linkWidth = l -> hotspots.contains(l.src.component) && hotspots.contains(l.dest.component)
						? 4
						: 1;
				customizer.componentStyle = c -> hotspots.contains(c) ? Style.filled : Style.solid;
				customizer.componentWidth = c -> 0.01;
				customizer.linkLabel = l -> null;
				customizer.outputFormats = List.of("dot", "pdf", "png");
				customizer.cfg = DOTCFG.POS;
			});
		}
	}

	public static void main(String[] args) throws Throwable {

		GraphvizDriver.path = "/usr/local/bin/";
		GNUPlot.path = "/usr/local/bin/";
		Idawi.setDirectory("$HOME/tmp/valentin").open();

		// declares the plots we will draw
		var trafficPlot = Idawi.plots.createPlot("Traffic", "time (s)", "#msg");
		var msgSentFct = trafficPlot.createFunction("#msg sent");
		var msgReceivedFct = trafficPlot.createFunction("#msg received");
		var trafficFct = trafficPlot.createFunction("trafficFct (bytes)");

		// create the n components that will be simulated
		var net = new CloudEdgeFog(20);

		// show what happens on stdout
//		RuntimeEngine.listeners.add(new RuntimeListener.StdOutRuntimeListener(System.out));

		// trigger measures
		var measureCollector = new TrafficListener() {
			long nbMsgReceived, incomingTraffic;

			@Override
			public void newMessageReceived(TransportService t, Message msg) {
				msgReceivedFct.instances(null).addMeasure(Idawi.agenda.now(), nbMsgReceived++);
				trafficFct.instances(null).addMeasure(Idawi.agenda.now(), incomingTraffic += msg.sizeOf());
			}
		};

		net.components.forEach(c -> c.trafficListeners.add(measureCollector));

		Component c0 = net.components.get(0);

		System.out.println("plot initial topology as component 0 sees it");
		net.plot(c0);

		net.components.forEach(c -> c.localView().disseminateTopologyChangesWhenTheyOccur = true);

		// trigger network plots
		net.components.forEach(c -> new TopologyChangePlotter(c, i -> true));

		// initiate mobility
//		RuntimeEngine.offer(new NewLinkEvent(1, root, RuntimeEngine.prng));

		// ask a node 0 to inject an item into the DHT
		net.components.forEach(c -> new ChordService(c));
		Idawi.agenda.scheduleAt(2, "add item",
				() -> c0.service(ChordService.class).store(new Item("item1", "value".getBytes())));

		// stop after 20s
		Idawi.agenda.setTerminationCondition(() -> Idawi.agenda.now() > 100);
		System.err.println("running");
		Idawi.agenda.start();

		Idawi.plots.gnuplot(true, true, 1, false, AVGMODE.IterativeMean, "linespoints");
	}

}
