package idawi.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Service;
import idawi.To;
import idawi.net.LMI;
import idawi.net.NetworkingService;
import idawi.service.ComponentDeployer;
import toools.thread.Q;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class Demo4_deploy_local_peers {
	public static void main(String[] args) throws IOException, InterruptedException {

		// creates the things in the local JVM
		List<Component> things = new ArrayList<>();
		Component initialThing = new Component(ComponentInfo.fromPDL("name=0"));
		initialThing.lookupService(ComponentDeployer.class).deployLocalPeers(50, true, peerOk -> things.add(peerOk));
		LMI.chain(things);
		Component first = things.get(0);
		Component last = things.get(things.size() - 1);

		// prints neighborhoods for all things
		things.forEach(t -> System.out.println(t + " => " + t.lookupService(NetworkingService.class).neighbors()));

		Q wait = new Q(1);

		// declares a new service that does nothing else than printing the route of
		// received messages
		class DummyService extends Service {
			public DummyService(Component t) {
				super(t);
				registerOperation(null, (msg, out) -> {
					System.out.println("message route: " + msg.route);
					wait.add_blocking("");
				});
			}
		}

		// install this service on all our things in the JVM
		last.addService(DummyService.class);
		// things.forEach(t -> t.services.add(new DummyService(t)));

		To to = new To(DummyService.class, null);
		first.lookupService(NetworkingService.class).send("hello!", to, null);
		wait.get_blocking();
		System.out.println("completed");
		Component.stopPlatformThreads();
	}
}
