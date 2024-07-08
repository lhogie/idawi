package idawi.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import idawi.Component;
import idawi.service.SimulatedLocationService;
import idawi.service.web.WebService;
import idawi.transport.Bluetooth;
import idawi.transport.Topologies;
import idawi.transport.WiFiDirect;
import toools.thread.Threads;

public class Centralized {
	public static void main(String[] args) throws IOException {
		var l = new ArrayList<Component>();

		int n = 10;
		var rand = new Random();

		for (int i = 0; i < n; ++i) {
			var a = new Component();
			new SimulatedLocationService(a);
			a.service(Bluetooth.class, true).emissionRange = rand.nextDouble(100);
			var ls = a.service(SimulatedLocationService.class, true);
			ls.location.x = rand.nextDouble(1000);
			ls.location.y = rand.nextDouble(1000);
			ls.angle = rand.nextDouble(2 * Math.PI);
			ls.speed = rand.nextDouble(1);
			l.add(a);
		}

		l.get(0).service(WebService.class).startHTTPServer(4456);
		System.out.println("gateway: " + l.get(0));

		var r = new Random();

		while (true) {
			for (var c : l) {
				// System.out.println(c + "\t" +
				// c.lookup(SimulatedLocationService.class).location);
			}

			Threads.sleep(1);

			var o = l.get(r.nextInt(l.size()));
			// System.out.println(o);

			for (var c : l) {
				c.service(SimulatedLocationService.class, true).move();
				c.service(SimulatedLocationService.class).angle += rand.nextDouble(Math.PI / 5 - Math.PI / 10);
			}

			Topologies.wirelessMesh(l, (from, to) -> WiFiDirect.class, l);
		}

	}
}
