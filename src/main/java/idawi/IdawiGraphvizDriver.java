package idawi;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import idawi.service.LocationService;
import idawi.service.local_view.Network;
import idawi.transport.Link;
import jdotgen.GraphVizNode;
import jdotgen.GraphVizNode.Position;
import jdotgen.GraphVizNode.Shape;
import jdotgen.GraphvizArc;
import jdotgen.GraphvizDriver;
import jdotgen.Props.Style;
import toools.Stop;

public class IdawiGraphvizDriver extends GraphvizDriver {
	final Network g;
	public Function<Component, Boolean> showComponent = c -> true;
	public Function<Link, Boolean> showLink = c -> true;

	public Function<Component, Integer> componentPenWidth = c -> 1;
	public Function<Component, String> componentLabel = c -> c.friendlyName;
	public Function<Component, String> componentID = c -> c.toString();
	public Function<Component, Shape> componentShape = c -> Shape.circle;
	public Function<Component, Style> componentStyle = c -> Style.solid;
	public Function<Component, Double> componentWidth = c -> 0.5;
	public Function<Component, Color> componentFillColor = c -> null;
	public Function<Link, Style> linkStyle = l -> l.src.component.isDigitalTwin() || l.dest.component.isDigitalTwin()
			? Style.dotted
			: Style.solid;
	public Function<Link, String> linkLabel = l -> l.getTransportName();
	public Function<Link, Integer> linkWidth = l -> 1;

	public IdawiGraphvizDriver(Network g) {
		this.g = g;
	}

	@Override
	protected void forEachVertex(Consumer<GraphVizNode> nodeConsumer) {
		g.forEachComponent(c -> {
			if (showComponent.apply(c)) {
				var n = new GraphVizNode();
				n.id = componentID.apply(c);
				n.penwidth = componentPenWidth.apply(c);
				n.width = componentWidth.apply(c);
				n.label = componentLabel.apply(c);
				n.shape = componentShape.apply(c);
				n.style = componentStyle.apply(c);
				n.fillColor = componentFillColor.apply(c);

				var ls = c.service(LocationService.class);

				if (ls != null) {
					n.position = new Position();
					n.position.x = ls.location.x;
					n.position.y = ls.location.y;
				} else {
					n.position = null;
				}

				additionalCustomizing(c, n);
				nodeConsumer.accept(n);
			}

			return Stop.no;
		});
	}

	@Override
	protected void forEachArc(Consumer<GraphvizArc> arcConsumer) {
		Set<Link> ignoreLinks = new HashSet<>();

		g.forEachLink(l -> {
			if (showLink.apply(l) && !ignoreLinks.contains(l)) {
				var a = new GraphvizArc();

				// search for the reverse link
				{
					var reverseLink = g.findALinkConnecting(l.dest, l.src);
					a.directed = reverseLink == null;

					if (!a.directed) {
						ignoreLinks.add(reverseLink);
					}
				}

				a.from = componentID.apply(l.src.component);
				a.to = componentID.apply(l.dest.component);
				a.style = linkStyle.apply(l);
				a.penwidth = linkWidth.apply(l);
				a.label = linkLabel.apply(l);
				additionalCustomizing(l, a);
				arcConsumer.accept(a);
			}

			return Stop.no;
		});
	}

	protected void additionalCustomizing(Component c, GraphVizNode n) {

	}

	protected void additionalCustomizing(Link l, GraphvizArc a) {
	}
}
