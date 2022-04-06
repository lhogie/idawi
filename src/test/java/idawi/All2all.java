package idawi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import idawi.net.LMI;
import idawi.net.NetworkingService;
import idawi.routing.RoutingScheme1;
import idawi.routing.RoutingScheme_bcast;
import idawi.service.PingService;
import idawi.service.ServiceManager;

public class All2all {
	public static void main(String[] args) throws RemoteException {
		new All2all().all2all();
	}

	@Test
	public void all2all() throws RemoteException {
//		MessageQueue.DEFAULT_TIMEOUT_IN_SECONDS = 1;
		var all = new HashSet<ComponentDescriptor>();

		System.out.println("Creating components");
		for (int i = 0; i < 10; ++i) {
			var c = new Component();
			if (c.lookupOperation(ServiceManager.has.class).has(RoutingScheme_bcast.class)) {
				c.lookupOperation(ServiceManager.stop.class).stop(RoutingScheme_bcast.class);
			}
			if (c.lookupOperation(ServiceManager.has.class).has(RoutingScheme1.class)) {
				c.lookupOperation(ServiceManager.stop.class).stop(RoutingScheme1.class);
			}
			c.lookupOperation(ServiceManager.start.class).f(RoutingScheme1.class);
			all.add(c.descriptor());
		}

		System.out.println("Connecting them");
		var componentList = new ArrayList<>(Component.componentsInThisJVM.values());
		Collections.sort(componentList, (a, b) -> a.descriptor().name.compareTo(b.descriptor().name));
//		LMI.randomTree(new ArrayList<>(Component.componentsInThisJVM.values()));
		LMI.chain(componentList);
		System.out.println(componentList);
//		LMI.clique(Component.componentsInThisJVM.values());

		System.out.println("messaging");
		AtomicLong n = new AtomicLong();
		var repliers = new HashMap<ComponentDescriptor, Message>();

		for (var c : componentList) {
			var allButMe = new HashSet<>(all);
			allButMe.remove(c.descriptor());
			System.out.println(c + " pings " + allButMe);
			new Component().lookup(PingService.class).ping().recv_sync(1, 1, collector -> {
				var msg = collector.messages.last();
				n.incrementAndGet();
				System.out.println(n.get() + ": " + msg);
				var sender = msg.route.source().component;
				Message previousMsg = repliers.get(sender);

				if (previousMsg != null) {
					System.err.println(sender + " already replied msg " + previousMsg);
				}

				repliers.put(sender, msg);
			});

			break;
		}

		int nbMsg = 0;

		for (var c : Component.componentsInThisJVM.values()) {
			nbMsg += c.lookup(NetworkingService.class).getNbMessagesReceived();
		}

		System.out.println("nbMessage received: " + nbMsg);
		System.out.println("nb repliers: " + repliers.size());

		System.out.println("done");
		Component.componentsInThisJVM.clear();
		Component.stopPlatformThreads();
	}
}
