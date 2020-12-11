package idawi.demo;

import java.io.IOException;
import java.net.UnknownHostException;

import idawi.Component;
import idawi.ComponentInfo;
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

public class Demo1_multi_jvm {
	public static void main(String[] args) throws IOException {
		final int basePort = 4000;

		ComponentInfo t1d = new ComponentInfo();
		t1d.friendlyName = "this jvm";
		t1d.tcpPort = 6677;
		Component t1 = new Component(t1d);

		ComponentInfo t2d = new ComponentInfo();
		t2d.friendlyName = "this jvm";
		t2d.tcpPort = 6678;

		t1.lookupService(ComponentDeployer.class).deployOtherJVM(t2d, true, fdbck -> System.out.println(fdbck),
				p -> System.out.println("ok"));
		
		Message pong = t1.lookupService(PingPong.class).ping(t2d, 1);
		System.out.println("pong: " + pong);

	}
}
