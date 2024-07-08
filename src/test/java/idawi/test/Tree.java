package idawi.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import idawi.Component;
import idawi.service.local_view.Network;
import idawi.service.web.WebService;
import idawi.transport.SharedMemoryTransport;
import toools.thread.Threads;

public class Tree {
	public static void main(String[] args) throws IOException {
		var components = new ArrayList<Component>();
		components.add(new Component());

		for (int i = 0; i < 4; ++i) {
			var a = components.get(new Random().nextInt(components.size()));
			var b = new Component();
			components.add(b);
			Network.markLinkActive(a, b, SharedMemoryTransport.class, false, components);
		}

		components.get(0).service(WebService.class).startHTTPServer();
		Threads.sleepForever();
	}
}
