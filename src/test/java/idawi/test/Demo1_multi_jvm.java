package idawi.test;

import java.io.IOException;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.ExtraJVMDeploymentRequest;
import idawi.knowledge_base.ComponentRef;
import idawi.messaging.Message;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class Demo1_multi_jvm {
	public static void main(String[] args) throws IOException {
		final int basePort = 4000;

		Component t1 = new Component(new ComponentRef("c1"));

		var req = new ExtraJVMDeploymentRequest();
		req.targetDescription.ref = new ComponentRef("c2");

		t1.lookup(DeployerService.class).deployInNewJVM(req, fdbck -> System.out.println(fdbck));

		Message pong = t1.bb().ping(req.targetDescription.ref).poll_sync();
		System.out.println("pong duration: " + pong.route.duration());
		System.out.println("pong message: " + pong);

	}
}
