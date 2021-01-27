package idawi.demo;

import java.io.IOException;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.service.DeployerService;
import idawi.service.PingService;

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

		ComponentDescriptor t1d = new ComponentDescriptor();
		t1d.friendlyName = "this jvm";
		t1d.tcpPort = 6677;
		Component t1 = new Component(t1d);

		ComponentDescriptor t2d = new ComponentDescriptor();
		t2d.friendlyName = "this jvm";
		t2d.tcpPort = 6678;

		t1.lookupService(DeployerService.class).deployOtherJVM(t2d, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));
		
		Message pong = t1.lookupService(PingService.class).ping(t2d, 1);
		System.out.println("pong: " + pong);

	}
}
