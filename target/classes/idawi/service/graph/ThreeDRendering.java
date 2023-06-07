package idawi.service.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.InnerClassOperation;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.service.Location;
import toools.thread.Threads;

public class ThreeDRendering extends Service {

	public class Vertex {
		public Component component;
		public Location location;
		public double size;
		public int color;
	}

	public class Edge {
		public Vertex from, to;
	}

	public class Graph {
		public List<Vertex> vertices = new ArrayList<>();
		public Set<Edge> edges = new HashSet<>();
		GraphListener l;

		public Vertex randomV() {
			return g.vertices.get(ThreadLocalRandom.current().nextInt(g.vertices.size()));
		}
	}

	Graph g = new Graph();

	public ThreeDRendering(Component component) {
		super(component);

		new Thread(() -> {
			while (true) {
				Threads.sleep(1);

				double r = ThreadLocalRandom.current().nextDouble();

				if (g.vertices.size() < 2 || r < 0.1) {
					var n = new Vertex();
					n.component = new Component("" + ThreadLocalRandom.current().nextInt(1000));
					g.vertices.add(n);
					g.l.newVertex(n);
				} else if (g.vertices.size() == 100 || r < 0.2) {
					var v = g.randomV();
					g.vertices.remove(v);
					g.l.newVertex(v);
				} else if (r < 0.4) {
					var e = new Edge();
					e.from = g.randomV();

					while (e.to == null || e.to == e.from) {
						e.to = g.randomV();
					}

					g.edges.add(e);
					g.l.newEdge(e);
				}
			}
		});
	}

	@Override
	public String webShortcut() {
		return "dyng";
	}

	interface GraphListener {
		void newVertex(Vertex v);

		void vertexRemoved(Vertex v);

		void newEdge(Edge e);

		void edgeRemoved(Edge e);
	}

	public static class VertexEvent {
		Vertex v;

		public VertexEvent(Vertex v) {
			this.v = v;
		}
	}

	public static class EdgeEvent {
		Edge e;

		public EdgeEvent(Edge e) {
			this.e = e;
		}
	}

	static class VertexRemoved extends VertexEvent {
		public VertexRemoved(Vertex v) {
			super(v);
		}
	}

	static class NewVertex extends VertexEvent {
		public NewVertex(Vertex v) {
			super(v);
		}
	}

	static class EdgeRemoved extends EdgeEvent {
		public EdgeRemoved(Edge v) {
			super(v);
		}
	}

	static class NewEdge extends EdgeEvent {
		public NewEdge(Edge v) {
			super(v);
		}
	}

	public class agetll extends InnerClassOperation {
		@Override
		public String getDescription() {
			return "get the whole graph";
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var tm = in.poll_sync();
			reply(tm, g);

			var l = new GraphListener() {

				@Override
				public void newVertex(Vertex v) {
					reply(tm, new NewVertex(v));
				}

				@Override
				public void vertexRemoved(Vertex v) {
					reply(tm, new VertexRemoved(v));
				}

				@Override
				public void newEdge(Edge e) {
					reply(tm, new NewEdge(e));
				}

				@Override
				public void edgeRemoved(Edge e) {
					reply(tm, new EdgeRemoved(e));
				}
			};
		}
	}
}