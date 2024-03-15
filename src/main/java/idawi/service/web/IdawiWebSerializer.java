package idawi.service.web;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import idawi.Component;
import idawi.routing.Route;
import jaseto.ArrayNode;
import jaseto.Jaseto;
import jaseto.Node;
import jaseto.ThrowableNode;

public class IdawiWebSerializer extends JasetoSerializer {
	public IdawiWebSerializer() {
		super(new Jaseto() {

			@Override
			public String classname(Class<?> o) {
				if (URLContentException.class.isAssignableFrom(o)) {
					return "URL error";
				} else if (List.class.isAssignableFrom(o)) {
					return "list";
				} else if (Set.class.isAssignableFrom(o)) {
					return "set";
				} else if (Collection.class.isAssignableFrom(o)) {
					return "collection";
				}

				return super.classname(o);
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

		});
	}
}
