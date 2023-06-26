package idawi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;

import idawi.service.LocationService;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class Dot {
	public static class DotDriver {
		Function<Component, String> shape = c -> "circle";
		Function<Component, Integer> size = c -> 10;
		Function<Link, String> edgeLabel = t -> t.src.getName();
		Function<Link, String> edgeStyle = t -> "plain";
	}

	public static String toSVG(Iterable<Component> components, DotDriver dotControls) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		toDot(components, pw, dotControls);
		pw.flush();
		return sw.toString();
	}

	
	public static String toDot(Iterable<Component> components, DotDriver dotControls) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		toDot(components, pw, dotControls);
		pw.flush();
		return sw.toString();
	}

	public static void toDot(Iterable<Component> components, PrintWriter out, DotDriver dot) {
		out.println("digraph {");

		for (var c : components) {
			out.println(c + " [");
			out.println("shape=" + dot.shape.apply(c));
			out.println(",size=" + dot.size.apply(c));

			var ls = c.need(LocationService.class);

			if (ls != null) {
				out.println(c + ", pos=\"" + ls.location.x + "," + ls.location.y + "!\"");
			}

			out.println(c + "];");
		}

		for (var c : components) {
			for (var t : c.services(TransportService.class)) {
				for (var n : t.outLinks()) {
					out.println(t.component + " -> " + n + " [");
					out.println(c + " [");
					out.println("label=" + dot.edgeLabel.apply(n));
					out.println("style=" + dot.edgeStyle.apply( n));
					out.println(c + "];");
				}
			}
		}

		out.println("}");
	}
}
