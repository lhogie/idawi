package idawi.demo;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import idawi.Component;
import idawi.Service;
import idawi.knowledge_base.DigitalTwinService;
import idawi.routing.BlindBroadcasting;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.TransportService;
import jdotgen.GraphvizDriver;
import toools.io.file.RegularFile;

public class ManyComponents {
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("start");

		// creates components
		var components = Component.create("c", 100);
		
		for (var c : components) {
			new SharedMemoryTransport(c);
			new DigitalTwinService(c);
			new BlindBroadcasting(c);
		}

		// connect them in a random tree
		SharedMemoryTransport.chainRandomly(components, 3, new Random(), SharedMemoryTransport.class);

		System.out.println(SharedMemoryTransport.toDot(components));
		var pdfFile = RegularFile.createTempFile("", ".pdf");
		GraphvizDriver.pathToCommands = "/usr/local/bin/";
		pdfFile.setContent(SharedMemoryTransport.toDot(components).toPDF());
		pdfFile.open();

		var first = components.get(0);
		var last = components.get(components.size() - 1);
		last.lookup(SharedMemoryTransport.class).connectTo(first);

		var q = first.bb().ping(components.get(components.size() / 2));
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
