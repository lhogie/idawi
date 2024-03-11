package idawi.test;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;

/**
 * 
 * @author lhogie
 *
 * 
 *
 */

public class Demo1_multi_jvm {
	public static void main(String[] args) throws Throwable {
		Component t1 = new Component();

		var c = t1.service(DeployerService.class).deployInNewJVM(fdbck -> System.out.println(fdbck));

		Message pong = t1.bb().ping(c).poll_sync();
		System.out.println("pong duration: " + pong.route.duration());
		System.out.println("pong message: " + pong);

	}
}
