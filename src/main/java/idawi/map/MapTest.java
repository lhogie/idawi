package idawi.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import idawi.ComponentDescriptor;
import idawi.service.rest.GSONSerializer;
import toools.collections.Collections;
import toools.io.JavaResource;

public class MapTest {
	public static void main(String[] args) {
		List<String> names = Arrays
				.asList(new String(new JavaResource(MapTest.class, "names.lst").getByteArray()).split("\n"));
		names = names.subList(0, 5);
		List<ComponentDescriptor> components = new ArrayList();

		for (String name : names) {
			ComponentDescriptor i = new ComponentDescriptor();
			i.friendlyName = name;
			components.add(i);
		}

		for (ComponentDescriptor a : components) {
			for (int i = 0; i < 3; ++i) {
				var b = Collections.pickRandomObject(components, new Random());
				a.neighbors.add(b.friendlyName);
			}
		}

		System.out.println(new String(new GSONSerializer().toBytes(components)));
	}
}
