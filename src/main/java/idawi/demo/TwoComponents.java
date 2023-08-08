package idawi.demo;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.RemotelyRunningEndpoint;
import idawi.service.DemoService;
import idawi.service.local_view.Network;
import idawi.transport.SharedMemoryTransport;

public class TwoComponents {
	public static void main(String[] args) throws IOException {
		var a = new Component("a");
		var b = new Component("b");

		Network.markLinkActive(a, b, SharedMemoryTransport.class, true, Set.of(a, b));
		RemotelyRunningEndpoint r = a.bb().exec(DemoService.class, DemoService.stringLength.class, "salut");
		System.out.println(r.returnQ.poll_sync().content);
	}
}
