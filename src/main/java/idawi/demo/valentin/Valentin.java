package idawi.demo.valentin;

import java.util.Random;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.routing.BlindBroadcasting;
import idawi.routing.RoutingListener;
import idawi.transport.Topologies;
import idawi.transport.WiFiDirect;
import jdotgen.GraphvizDriver;
import toools.io.file.Directory;

public class Valentin {

	public static void main(String[] args) throws Throwable {
		// RuntimeEngine.syncToI3S();
		GraphvizDriver.pathToNativeExecutables = "/usr/local/bin/";
		Random prng = new Random(5);

		RuntimeEngine.terminationCondition = () -> RuntimeEngine.now() > 20;
//		RuntimeEngine.listeners.add(new StdOutRuntimeListener());

		// generates a random topology of simulated components
		Component root = new Component("root");
		root.localView().createTwins(10);
		var c0 = root.localView().lookup("0");
		System.out.println("components: " + root.localView().components());
		
		
		Topologies.chainRandomly(root.localView().components(), 2, prng, WiFiDirect.class, (a, b) -> true);

		root.localView().components()
				.forEach(c -> c.need(BlindBroadcasting.class).listeners.add(RoutingListener.stdout));
		root.localView().components().forEach(c -> new ChordService(c));

		// each second a new topology change will happen
		RuntimeEngine.offer(new NewLinkEvent(1, root, prng));


		root.localView().bfs(c0).predecessors.allPaths().forEach(r -> System.out.println("route: " + r));

		// ask a node 0 to inject an item into the DHT
		RuntimeEngine.offer(2, () -> c0.need(ChordService.class).store(new Item("item1", "value".getBytes())));

		// results will go there
		var dir = new Directory("$HOME/tmp/valentin");

		if (dir.exists()) {
			dir.deleteRecursively();
		}

		// each mobility event will entail the generation of a new image of the network
		RuntimeEngine.plotNet(root.localView(), dir, e -> e instanceof MobilityEvent);
		dir.open();

		System.out.println(RuntimeEngine.blockUntilSimulationHasCompleted() + " event(s) processed");
	}

}
