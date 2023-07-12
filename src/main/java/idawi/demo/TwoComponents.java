package idawi.demo;

import java.io.IOException;

import idawi.Component;
import idawi.RemotelyRunningEndpoint;
import idawi.service.DemoService;
import idawi.service.local_view.Network;
import idawi.transport.SharedMemoryTransport;

public class TwoComponents {
	public static void main(String[] args) throws IOException {
		var a = new Component("a");
		var b = new Component("b");

		Network.link(a, b, SharedMemoryTransport.class, true);
		RemotelyRunningEndpoint r = a.bb().exec(DemoService.class, DemoService.stringLength.class, "salut");
		System.out.println(r.returnQ.poll_sync().content);
	}
}
