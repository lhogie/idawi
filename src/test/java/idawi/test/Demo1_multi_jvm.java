package idawi.test;

import java.io.IOException;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.deploy.DeployerService.ExtraJVMDeploymentRequest;
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
		Component t1 = new Component();

		var req = new ExtraJVMDeploymentRequest();
		req.target = new Component();

		t1.service(DeployerService.class).deployInNewJVM(req, fdbck -> System.out.println(fdbck));

		Message pong = t1.bb().ping(req.target).poll_sync();
		System.out.println("pong duration: " + pong.route.duration());
		System.out.println("pong message: " + pong);

	}
}
