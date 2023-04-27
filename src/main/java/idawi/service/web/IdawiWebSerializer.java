package idawi.service.web;

import idawi.Component;
import idawi.routing.Route;
import jaseto.DefaultCustomizer;
import jaseto.Node;

public class IdawiWebSerializer extends JasetoSerializer {
	public IdawiWebSerializer() {
		j.customizer = new DefaultCustomizer() {

			@Override
			public Object substitute(Object o) {
				if (o instanceof Component) {
					return o.toString();
				} else if (o instanceof Route) {
					return ((Route) o).components().stream().map(c -> c.ref).toList();
				}

				return o;
			}

			//@Override
			public Node alter(Node n) {
				if (!n.path().equals(".content")) {
					// n.removeKey("#class");
				}

				/*
				 * if (//n.value instanceof Collection) { var on = (ObjectNode) n; var elements
				 * = (ArrayNode) on.map.remove("elements"); elements.children.forEach(c ->
				 * on.map.put("", c)); }
				 */

				return n;
			}
		};
	}
}
