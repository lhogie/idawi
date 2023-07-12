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
		var l = new ArrayList<Component>();
		l.add(new Component());

		for (int i = 0; i < 4; ++i) {
			var a = l.get(new Random().nextInt(l.size()));
			var b = new Component();
			l.add(b);
			Network.link(a, b, SharedMemoryTransport.class, false);
		}

		l.get(0).need(WebService.class).startHTTPServer();
		Threads.sleepForever();
	}
}
