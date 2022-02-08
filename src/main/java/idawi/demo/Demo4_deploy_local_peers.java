package idawi.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.MessageQueue;
import idawi.Service;
import idawi.To;
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
			registerOperation(new op());
		}

		public class op extends InnerOperation {
			@Override
			public void exec(MessageQueue in) throws Throwable {
				var msg = in.get_blocking();
				System.out.println("message route: " + msg.route);
				wait.add_blocking("");
			}

			@Override
			public String getDescription() {
				// TODO Auto-generated method stub
				return null;
			}
		}

	}

	public static void main(String[] args) throws IOException, InterruptedException {

		// creates 50 components in the local JVM
		List<Component> things = new ArrayList<>();
		Component initialThing = new Component(ComponentDescriptor.fromCDL("name=0"));
		initialThing.lookup(DeployerService.class).deployInThisJVM(50, i -> "c" + i, true,
				peerOk -> things.add(peerOk));
		LMI.chain(things);
		Component first = things.get(0);
		Component last = things.get(things.size() - 1);

		// prints neighborhoods for all things
		things.forEach(t -> System.out.println(t + " => " + t.lookup(NetworkingService.class).neighbors()));

		// install this service on the last component
		var s = new DummyService(last);
		// things.forEach(t -> t.services.add(new DummyService(t)));

		var to = new To(last.descriptor()).o(DummyService.op.class);
		first.lookup(NetworkingService.class).exec(to, true, "hello!");
		s.wait.get_blocking();
		System.out.println("completed");
		Component.stopPlatformThreads();
	}
}
