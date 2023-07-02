package idawi.demo.valentin;

import java.io.IOException;
import java.util.Random;

import idawi.Component;
import idawi.Event;
import idawi.RuntimeEngine;
import idawi.RuntimeListener;
import idawi.Service;
import idawi.SpecificTime;
import idawi.transport.Topologies;
import idawi.transport.WiFiDirect;
import jdotgen.GraphvizDriver;
import toools.io.file.Directory;

public class Valentin {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		// RuntimeEngine.syncToI3S();
		GraphvizDriver.pathToCommands = "/usr/local/bin/";
		Random prng = new Random();
		RuntimeEngine.simulationMode();

		// generates a random topology of simulated components
		Component root = new Component("root");
		root.localView().createTwins(10);
		Topologies.chainRandomly(root.localView().components(), 3, prng, WiFiDirect.class, (a, b) -> true);

		// results will go there
		var dir = new Directory("$HOME/tmp/valentin");

		if (dir.exists()) {
			dir.deleteRecursively();
		}

		// each mobility event will entail the generation of a new image of the network
		RuntimeEngine.plotNet(root.localView(), dir, e -> e instanceof MobilityEvent);
		dir.open();

		RuntimeEngine.listeners.add(new RuntimeListener() {

			@Override
			public void newEvent(Event<?> e) {
				System.out.println("new event executed: " + e);
			}

			@Override
			public void eventSubmitted(Event<SpecificTime> newEvent) {
				System.out.println("new event submitted: " + newEvent);
			}
		});

		// each second a new topology change will happen
		RuntimeEngine.eventQueue.offer(new NewLinkEvent(Service.now() + 1, root.localView().components(), prng));
		RuntimeEngine.eventQueue.offer(new LinkFailEvent(Service.now() + 2, root, prng));
	}

}
