package idawi.demo;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import idawi.Component;
import idawi.RuntimeEngine;
import idawi.routing.BlindBroadcasting;
import idawi.service.local_view.LocalViewService;
import idawi.service.local_view.Network;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;
import idawi.transport.TransportService;
import jdotgen.GraphvizDriver;
import toools.io.file.RegularFile;

public class ManyComponents {
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("start");

		// creates components
		var components = Component.createNComponent("c", 100);

		for (var c : components) {
			new SharedMemoryTransport(c);
			new LocalViewService(c);
			new BlindBroadcasting(c);
		}

		// connect them in a random tree
		Topologies.chainRandomly(components, 3, new Random(), SharedMemoryTransport.class, (a, b) -> true);

//		System.out.println(Topologies.toDot(components));
		var pdfFile = RegularFile.createTempFile("", ".pdf");
		GraphvizDriver.path = "/usr/local/bin/";
//		pdfFile.setContent(Topologies.toDot(components).toPDF());
		pdfFile.open();

		var first = components.get(0);
		var last = components.get(components.size() - 1);
		Network.link(last, first, SharedMemoryTransport.class, false);

		var q = first.bb().ping(components.get(components.size() / 2));
		var pong = q.poll_sync();
		System.out.println("pong= " + pong);

		long nbMessages = 0;

		for (var c : components) {
			nbMessages += c.need(TransportService.class).nbOfMsgReceived;
		}

		RuntimeEngine.threadPool.awaitTermination(1, TimeUnit.SECONDS);
		System.out.println("nbMessages: " + nbMessages);
	}
}
