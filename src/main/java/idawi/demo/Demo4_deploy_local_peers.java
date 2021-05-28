package idawi.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.ComponentAddress;
import idawi.ComponentDescriptor;
import idawi.IdawiOperation;
import idawi.MessageQueue;
import idawi.Service;
import idawi.net.LMI;
import idawi.net.NetworkingService;
import idawi.service.DeployerService;
import toools.thread.Q;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class Demo4_deploy_local_peers {
	// declares a new service that does nothing else than printing the route of
	// received messages
	static class DummyService extends Service {
		Q wait = new Q(1);

		public DummyService(Component t) {
			super(t);
		}

		public static OperationID op;

		@IdawiOperation
		public void op(MessageQueue q) {
			var msg = q.get_blocking();
			System.out.println("message route: " + msg.route);
			wait.add_blocking("");
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		// creates 50 components in the local JVM
		List<Component> things = new ArrayList<>();
		Component initialThing = new Component(ComponentDescriptor.fromCDL("name=0"));
		initialThing.lookupService(DeployerService.class).deployInThisJVM(50, i -> "c" + i, true,
				peerOk -> things.add(peerOk));
		LMI.chain(things);
		Component first = things.get(0);
		Component last = things.get(things.size() - 1);

		// prints neighborhoods for all things
		things.forEach(t -> System.out.println(t + " => " + t.lookupService(NetworkingService.class).neighbors()));

		// install this service on the last component
		var s = new DummyService(last);
		// things.forEach(t -> t.services.add(new DummyService(t)));

		var to = new ComponentAddress(Set.of(last.descriptor()));
		first.lookupService(NetworkingService.class).exec(to, DummyService.op, true, "hello!");
		s.wait.get_blocking();
		System.out.println("completed");
		Component.stopPlatformThreads();
	}
}
