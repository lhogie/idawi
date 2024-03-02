package idawi.service.web;

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
				}

				return super.classname(o);
			}

			@Override
			public Node createNode(Object o) {
				if (o instanceof Route) {
					return new ArrayNode(((Route) o).components().stream().map(c -> c.toString()).toArray(), this);
				} else if (o instanceof URLContentException) {
					var node = new ThrowableNode(o, this);
					node.removeKey(ThrowableNode.STACK_TRACE);
					return node;
				} else if (o instanceof Component) {
					return toNode(o.toString());
				}

				return super.createNode(o);
			}

		});
	}
}
