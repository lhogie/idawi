package idawi.demo;

import java.util.ArrayList;
import java.util.Set;

import idawi.Component;
import idawi.Idawi;
import idawi.Service;
import idawi.routing.BlindBroadcasting;
import idawi.routing.ComponentMatcher;
import idawi.routing.RoutingService;
import idawi.routing.RoutingService.testEndpoint;
import idawi.service.LocationService;
import idawi.service.local_view.LocalViewService;
import idawi.transport.Topologies;
import idawi.transport.WiFiDirect;
import jdotgen.GraphvizDriver;
import jdotgen.GraphvizDriver.DOTCFG;
import jdotgen.Props.Style;
import toools.io.file.Directory;

public class PlotLargeNetwork {
	public static void main(String[] args) throws Throwable {
		Idawi.directory = new Directory("$HOME/idawi/test");
		Idawi.directory.mkdirs();
		Idawi.agenda.start();

		GraphvizDriver.path = "/usr/local/bin/";

		var r = new ArrayList<Component>();

		for (int i = 0; i < 50; ++i) {
			var c = new Component();
			r.add(c);
			c.service(BlindBroadcasting.class, true);
		}

		var owner = r.get(0);

		for (int i = 1; i < 50; ++i) {
			r.get(i).turnToDigitalTwin(owner);
		}

		LocationService.assignRandomLocations(r, 1000, 1000, 40);
		Topologies.wirelessMesh(r, (from, to) -> WiFiDirect.class, Set.of(r.get(0)));
//		Topologies.dchain(r, (from, to) -> WiFiDirect.class, Set.of(r.get(0)));

		r.get(0).defaultRoutingProtocol().exec(ComponentMatcher.all, RoutingService.class, testEndpoint.class, null);

		Idawi.enableEncryption = false;
		Idawi.agenda.stopWhen(() -> Idawi.agenda.time() >= 5, () -> {
			System.out.println("plotting");

			r.get(0).service(LocalViewService.class).g.plot("test", d -> {
				d.cfg = DOTCFG.POS;
				d.linkLabel = l -> l.nbMsgs == 0 ? "" : "" + l.nbMsgs;
				d.linkStyle = l -> l.nbMsgs == 0 ? Style.dotted : Style.solid;
//				d.componentWidth = c -> 0.01d;
				d.componentPenWidth = c -> 1;
				d.componentStyle = c -> c.alreadyReceivedMsgs.size() == 0 ? Style.dotted : Style.solid;
			});

			Idawi.directory.open();
		});
		System.out.println("started");
		System.out.println("stopped");

	}
}
