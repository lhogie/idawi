package idawi.demo;

import java.net.InetAddress;
import java.util.Set;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Graph;
import idawi.Message;
import idawi.service.ComponentDeployer;
import idawi.service.PingPong;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class Demo_deploymentPlan {
	public static void main(String[] args) throws Throwable {

		// creates a *local* peer that will drive the deployment
		Component t = new Component(ComponentInfo.fromCDL("name=master"));

		ComponentInfo musclotte = ComponentInfo.fromCDL("ssh=musclotte.inria.fr");
		ComponentInfo nicoati = ComponentInfo.fromCDL("ssh=nicoati.inria.fr");
		ComponentInfo dronic = ComponentInfo.fromCDL("ssh=dronic.i3s.unice.fr");
		ComponentInfo t2 = ComponentInfo.fromCDL("name=t2");
		ComponentInfo t3 = ComponentInfo.fromCDL("name=t3");
		ComponentInfo t4 = ComponentInfo.fromCDL("name=t4");
		ComponentInfo jvm2 = ComponentInfo.fromCDL("name=jvm1 / where=new_jvm");

		Graph<ComponentInfo> g = new Graph<>();
		g.add(t.descriptor(), musclotte);
		g.add(t.descriptor(), nicoati);
		g.add(t.descriptor(), dronic);
		g.add(t.descriptor(), t2);
		g.add(t.descriptor(), t3);
		g.add(t.descriptor(), t4);
		g.add(t.descriptor(), jvm2);

		System.out.println(g.bfs(t.descriptor()));

		t.lookupService(ComponentDeployer.class).apply(g, 10, true,feedback -> System.out.println(feedback), (p) -> System.out.println(p));

		// describes the child peer that will be deployed to
		ComponentInfo child = new ComponentInfo();
		InetAddress childHost = InetAddress.getByName(args[0]);
		child.inetAddresses.add(childHost);
		child.friendlyName = childHost.getHostName();
		child.sshParameters.hostname = childHost.getHostName();

		// deploy
		t.lookupService(ComponentDeployer.class).deploy(Set.of(child), true, 10000, true,
				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));

		// at this step the child is running on the remote host. We can interact with
		// it.
		long pingTime = System.currentTimeMillis();
		Message pong = t.lookupService(PingPong.class).ping(child, 1000);

		if (pong == null) {
			System.err.println("ping timeout");
		} else {
			long pongDuration = System.currentTimeMillis() - pingTime;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}
