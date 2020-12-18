package idawi.routing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import idawi.ComponentInfo;
import idawi.service.rest.GSONSerializer;
import toools.collections.Collections;
import toools.io.JavaResource;

public class MapTest {
	public static void main(String[] args) {
		List<String> names = Arrays
				.asList(new String(new JavaResource(MapTest.class, "names.lst").getByteArray()).split("\n"));
		names = names.subList(0, 5);
		List<ComponentInfo> components = new ArrayList();

		for (String name : names) {
			ComponentInfo i = new ComponentInfo();
			i.friendlyName = name;
			components.add(i);
		}

		for (ComponentInfo a : components) {
			for (int i = 0; i < 3; ++i) {
				var n = new ComponentInfo.Neighbor();
				var b = Collections.pickRandomObject(components, new Random());
				n.id = b.friendlyName;
				n.latency = 1;
				n.rate = 1;
				a.neighbors.add(n);
			}
		}

		System.out.println(new String(new GSONSerializer().toBytes(components)));
	}
}
