package idawi.test;

import idawi.Component;
import idawi.deploy.DeployerService;
import idawi.messaging.Message;
import idawi.service.PingService;

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

		var c = t1.service(DeployerService.class).newLocalJVM();

		Message pong = t1.service(PingService.class).ping(c);
		System.out.println("pong duration: " + pong.route.duration());
		System.out.println("pong message: " + pong);

	}
}
