package idawi.test;

import java.util.ArrayList;
import java.util.List;

import idawi.Component;
import idawi.Idawi;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.test.Demo4_deploy_local_peers.DummyService.op;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;
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

		public class op extends InnerClassEndpoint<Void, Void> {
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

	public static void main(String[] args) throws Throwable {

		// creates 50 components in the local JVM
		Component initialThing = new Component();
		List<Component> things = new ArrayList<>();
		var l = Component.createNComponent(50);
		Topologies.chain(things, (a, b) -> SharedMemoryTransport.class, things);
		Component last = things.get(things.size() - 1);

		// prints neighborhoods for all things
		things.forEach(t -> System.out.println(t + " => " + t.outLinks()));

		// install this service on the last component
		var s = new DummyService(last);
		// things.forEach(t -> t.services.add(new DummyService(t)));

		initialThing.defaultRoutingProtocol().exec(ComponentMatcher.unicast(last), DummyService.class, op.class, null);
		s.wait.poll_sync();
		System.out.println("completed");
		Idawi.agenda.stopNow(null);
	}
}
