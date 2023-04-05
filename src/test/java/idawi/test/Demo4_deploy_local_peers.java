package idawi.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.InnerClassOperation;
import idawi.Service;
import idawi.deploy.DeployerService;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.transport.SharedMemoryTransport;
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

		public class op extends InnerClassOperation {
			@Override
			public void impl(MessageQueue in) throws Throwable {
				var msg = in.poll_sync();
				System.out.println("message route: " + msg.route);
				wait.add_sync("");
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
		Component initialThing = new Component("0");
		List<Component> things = new ArrayList<>();
		var l = Component.create("c-", 50);
		initialThing.lookup(DeployerService.class).deployInThisJVM(l, peerOk -> things.add(peerOk));
		SharedMemoryTransport.chain(things, SharedMemoryTransport.class);
		Component last = things.get(things.size() - 1);

		// prints neighborhoods for all things
		things.forEach(t -> System.out.println(t + " => " + t.neighbors()));

		// install this service on the last component
		var s = new DummyService(last);
		// things.forEach(t -> t.services.add(new DummyService(t)));

		initialThing.defaultRoutingProtocol().exec(DummyService.op.class, null, ComponentMatcher.one(last), true,
				"hello!");
		s.wait.poll_sync();
		System.out.println("completed");
		Component.stopPlatformThreads();
	}
}
