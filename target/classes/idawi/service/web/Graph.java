package idawi.service.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Graph implements Serializable {
	static class Node implements Serializable {
		String color;
		String label;
	}

	static class Link implements Serializable {
		String style, color;
		Node a, b;
		boolean directed;
	}

	Set<Link> links = new HashSet<>();
	Set<Node> nodes = new HashSet<>();

	static List<String> colors = List.of("red", "blue", "green");
	static List<String> styles = List.of("plain", "dotted");

	public static Graph random() {
		Random r = new Random();
		return random(r.nextInt(20), r.nextInt(4));
	}

	public static Graph random(int nbVertices, int degree) {
		Graph g = new Graph();
		Random r = new Random();
		int nbLinks = degree * nbVertices;

		for (int i = 0; i < nbVertices; ++i) {
			Node n = new Node();
			n.color = colors.get(r.nextInt(colors.size()));
			n.label = "" + i;
			g.nodes.add(n);
		}

		var nodes = new ArrayList<>(g.nodes);

		for (int i = 0; i < nbLinks; ++i) {
			var l = new Link();
			l.color = colors.get(r.nextInt(colors.size()));
			l.style = styles.get(r.nextInt(styles.size()));
			l.a = nodes.get(r.nextInt(nodes.size()));
			l.b = nodes.get(r.nextInt(nodes.size()));
			l.directed = r.nextBoolean();
			g.links.add(l);
		}

		return g;
	}
}
