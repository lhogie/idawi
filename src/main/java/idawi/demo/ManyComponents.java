package idawi.demo;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import idawi.Component;
import idawi.Service;
import idawi.knowledge_base.ComponentRef;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.TransportService;

public class ManyComponents {
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("start");

		// creates references
		var refs = ComponentRef.create("c", 100);

		// creates components using these references
		var components = refs.stream().map(r -> new Component(r)).toList();

		// connect them in a random tree
		SharedMemoryTransport.chainRandomly(components, 3, new Random(), SharedMemoryTransport.class);

		var map = SharedMemoryTransport.createMap(SharedMemoryTransport.class, components);

		System.out.println(map.toDot());

		var first = components.get(0);
		var last = components.get(components.size() - 1);
		last.lookup(SharedMemoryTransport.class).connectTo(first);

		var q = first.bb().ping(components.get(components.size() / 2).ref());
		var pong = q.poll_sync();
		System.out.println("pong= " + pong);

		long nbMessages = 0;

		for (var c : components) {
			nbMessages += c.lookup(TransportService.class).nbOfMsgReceived;
		}

		Service.threadPool.awaitTermination(1, TimeUnit.SECONDS);
		System.out.println("nbMessages: " + nbMessages);
	}
}
