package idawi.demo;

import java.net.InetAddress;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.deploy.DeployerService;
import idawi.deploy.DeploymentPlan;
import idawi.service.PingService;

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
		Component t = new Component(ComponentDescriptor.fromCDL("name=master"));

		ComponentDescriptor musclotte = ComponentDescriptor.fromCDL("ssh=musclotte.inria.fr");
		ComponentDescriptor nicoati = ComponentDescriptor.fromCDL("ssh=nicoati.inria.fr");
		ComponentDescriptor dronic = ComponentDescriptor.fromCDL("ssh=dronic.i3s.unice.fr");
		ComponentDescriptor t2 = ComponentDescriptor.fromCDL("name=t2");
		ComponentDescriptor t3 = ComponentDescriptor.fromCDL("name=t3");
		ComponentDescriptor t4 = ComponentDescriptor.fromCDL("name=t4");
		ComponentDescriptor jvm2 = ComponentDescriptor.fromCDL("name=jvm1 / where=new_jvm");

		var plan = new DeploymentPlan();
		plan.g.addArc(t.descriptor(), musclotte);
		plan.g.addArc(t.descriptor(), nicoati);
		plan.g.addArc(t.descriptor(), dronic);
		plan.g.addArc(t.descriptor(), t2);
		plan.g.addArc(t.descriptor(), t3);
		plan.g.addArc(t.descriptor(), t4);
		plan.g.addArc(t.descriptor(), jvm2);

		System.out.println(plan.g.bfs(t.descriptor()));

		t.lookup(DeployerService.class).apply(plan, 10, rsyncOut -> System.out.println("rsync: " + rsyncOut),
				rsyncErr -> System.err.println("rsync: " + rsyncErr), feedback -> System.out.println(feedback),
				(p) -> System.out.println(p));

		// describes the child peer that will be deployed to
		ComponentDescriptor child = new ComponentDescriptor();
		InetAddress childHost = InetAddress.getByName(args[0]);
		child.inetAddresses.add(childHost);
		child.name = childHost.getHostName();
		child.ssh.host = childHost.getHostName();

		// deploy
		t.lookup(DeployerService.class).deploy(Set.of(child), true, 10000,
				rsyncOut -> System.out.println("rsync: " + rsyncOut),
				rsyncErr -> System.err.println("rsync: " + rsyncErr),
				feedback -> System.out.println("feedback: " + feedback), ok -> System.out.println("peer ok: " + ok));

		// at this step the child is running on the remote host. We can interact with
		// it.
		long pingTime = System.currentTimeMillis();
		Message pong = t.lookup(PingService.class).ping(child);

		if (pong == null) {
			System.err.println("ping timeout");
		} else {
			long pongDuration = System.currentTimeMillis() - pingTime;
			System.out.println("pong received after " + pongDuration + "ms");
		}
	}
}
