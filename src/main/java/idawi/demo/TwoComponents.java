package idawi.demo;

import java.io.IOException;

import idawi.Component;
import idawi.RemotelyRunningEndpoint;
import idawi.service.DemoService;
import idawi.transport.SharedMemoryTransport;

public class TwoComponents {
	public static void main(String[] args) throws IOException {
		var a = new Component("a");
		var b = new Component("b");

		a.need(SharedMemoryTransport.class).inoutTo(b);
		RemotelyRunningEndpoint r = a.bb().exec(DemoService.class, DemoService.stringLength.class, "salut");
		System.out.println(r.returnQ.poll_sync().content);
	}
}
