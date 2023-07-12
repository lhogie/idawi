package idawi.demo.valentin;

import java.util.stream.IntStream;

import idawi.Component;
import idawi.Event;
import idawi.RuntimeAdapter;
import idawi.RuntimeEngine;
import idawi.RuntimeListener;
import idawi.Utils;
import idawi.service.local_view.NetworkTopologyListener;
import idawi.transport.Link;
import idawi.transport.Topologies;
import idawi.transport.TransportService;
import idawi.transport.WiFiDirect;
import jdotgen.GraphvizDriver;
import jexperiment.AVGMODE;
import jexperiment.GNUPlot;
import toools.io.file.RegularFile;

public class Valentin {

	public static void main(String[] args) throws Throwable {

		GraphvizDriver.path = "/usr/local/bin/";
		GNUPlot.path = "/usr/local/bin/";
		RuntimeEngine.setDirectory("$HOME/tmp/valentin").open();

		// declares the plots we will draw
		var trafficPlot = RuntimeEngine.plots.createPlot("Traffic", "time (s)", "#msg");
		var msgSentFct = trafficPlot.createFunction("#msg sent");
		var msgReceivedFct = trafficPlot.createFunction("#msg received");

		int n = 10;
		// create the 10 components that will be simulated
		var components = IntStream.range(0, n).mapToObj(i -> new Component("" + i)).toList();
		System.out.println("components: " + components);

		// show what happens on stdout
//		RuntimeEngine.listeners.add(new RuntimeListener.StdOutRuntimeListener(System.out));

		// trigger measures
		RuntimeEngine.listeners.add(new RuntimeAdapter() {

			@Override
			public void eventProcessingCompleted(Event<?> e) {
				if (true)
					return;
				long nbMsgSent = 0, nbMsgReceived = 0;

				for (var c : components) {
					for (var r : c.services(TransportService.class)) {
						nbMsgSent += r.nbMsgSent;
						nbMsgReceived += r.nbOfMsgReceived;
					}
				}

				msgSentFct.instances(null).addMeasure(RuntimeEngine.now(), nbMsgSent);
				msgReceivedFct.instances(null).addMeasure(RuntimeEngine.now(), nbMsgReceived);
			}
		});

		// trigger network plots
		components.forEach(c -> new TopologyChangePlotter(c, i -> true));

		// generates a random topology
		Topologies.gnp(components, 0.5, WiFiDirect.class, true, 1);

		Component c0 = components.get(0);

		// initiate mobility
//		RuntimeEngine.offer(new NewLinkEvent(1, root, RuntimeEngine.prng));

		// ask a node 0 to inject an item into the DHT
		// components.forEach(c -> new ChordService(c));
		// RuntimeEngine.offer(2, "add item", () ->
		// c0.need(ChordService.class).store(new Item("item1", "value".getBytes())));

		// stop after 20s
		RuntimeEngine.terminated = () -> RuntimeEngine.now() > 100;
		System.err.println("running");
		RuntimeEngine.run();

		RuntimeEngine.plots.gnuplot(true, true, 1, false, AVGMODE.IterativeMean, "linespoints");
	}

}
