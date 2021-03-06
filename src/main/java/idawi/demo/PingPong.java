package idawi.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.Service;
import idawi.net.LMI;
import idawi.service.DeployerService;
import idawi.service.PingService;

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
		things.add(new Component(ComponentDescriptor.fromCDL("name=root")));

		for (int i = 1; i < 350; ++i) {
			// Thing t = things.get(ThreadLocalRandom.current().nextInt(things.size()));
			Component t = things.get(i - 1);

			ComponentDescriptor newPeer = new ComponentDescriptor();
			newPeer.friendlyName = "t" + i;

			// gets the deployment service
			DeployerService deployer = t.lookupService(DeployerService.class);

			// and asks it to deploy a new thing within the JVM
			List<Component> newThings = deployer.deploy(Set.of(newPeer), true, 1d, true, msg -> System.out.println(msg),
					ok -> System.out.println(ok + " is ready"));

			// registers the newly available thing in our list of things
			assert newThings.size() == 1;
			things.add(newThings.get(0));
		}

		LMI.chain(things);

		Component first = things.get(0);
		Component last = things.get(things.size() - 1);

		Message pong = PingService.ping(new Service(first), last.descriptor(), 10);

		assert pong.route.source().component.friendlyName.equals(last.friendlyName);
		System.out.println("***  " + pong.route);

	}
}
