package idawi.demo;

import java.io.IOException;

import idawi.Component;
import idawi.transport.SharedMemoryTransport;

public class Minimal {
	public static void main(String[] args) throws IOException {
		var a = new Component("a");
		var b = new Component("b");

		a.lookup(SharedMemoryTransport.class).connectTo(b);
		b.lookup(SharedMemoryTransport.class).connectTo(a);
		var q = a.bb().ping(b);
		var pong = q.poll_sync();
		System.out.println("pong= " + pong);
	}
}
