package idawi.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import idawi.Component;
import idawi.net.LMI;
import idawi.service.ServiceManager;
import idawi.service.rest.RESTService;
import toools.thread.Threads;

public class Tree {
	public static void main(String[] args) throws IOException {
		var l = new ArrayList<Component>();
		l.add(new Component());

		for (int i = 0; i < 4; ++i) {
			var a = l.get(new Random().nextInt(l.size()));
			var b = new Component();
			l.add(b);
			LMI.connect(a, b);
		}
		
		l.get(0).lookupO(ServiceManager.ensureStarted.class).f(RESTService.class);
		l.get(0).lookup(RESTService.class).startHTTPServer();
		Threads.sleepForever();
	}
}
