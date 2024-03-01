package idawi.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class PingPong {
	public static void main(String[] args) throws IOException {

		// creates the things in the local JVM
		List<Component> things = new ArrayList<>();
		things.add(new Component());

		for (int i = 1; i < 350; ++i) {
			// Thing t = things.get(ThreadLocalRandom.current().nextInt(things.size()));
			Component t = things.get(i - 1);

			// gets the deployment service
			DeployerService deployer = t.service(DeployerService.class);

			var c = new Component();
			c.friendlyName = "t" + i;

			// and asks it to deploy a new thing within the JVM
			List<Component> newThings = deployer.deployInThisJVM(Set.of(c), ok -> System.out.println(ok + " is ready"));

			// registers the newly available thing in our list of things
			assert newThings.size() == 1;
			things.add(newThings.get(0));
		}

		Topologies.chain(things, (a, b) -> SharedMemoryTransport.class, things);

		Component first = things.get(0);
		Component last = things.get(things.size() - 1);

		Message pong = first.bb().ping(last).poll_sync();

		assert pong.sender().equals(last);
		System.out.println("***  " + pong.route);

	}
}
