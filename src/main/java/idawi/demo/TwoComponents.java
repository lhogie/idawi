package idawi.demo;

import java.io.IOException;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.transport.SharedMemoryTransport;

public class TwoComponents {
	public static void main(String[] args) throws IOException {
		var a = new Component(new ComponentRef("a"));
		var b = new Component(new ComponentRef("b"));

		a.lookup(SharedMemoryTransport.class).connectTo(b);
		b.lookup(SharedMemoryTransport.class).connectTo(a);
		var q = a.bb().ping(b.ref());
		var pong = q.poll_sync();
		System.out.println("pong= " + pong);
	}
}
