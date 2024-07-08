package idawi.service.web;

import java.util.Collection;
import java.util.List;

import idawi.Component;
import idawi.routing.Route;
import jaseto.ArrayNode;
import jaseto.Jaseto;
import jaseto.Node;
import jaseto.ThrowableNode;
import toools.text.TextUtilities;

public class IdawiJasetoSerializer extends JasetoSerializer {
	public IdawiJasetoSerializer() {
		super(new Jaseto() {

			@Override
			public String classname(Class<?> o) {
				if (URLContentException.class.isAssignableFrom(o)) {
					return "URL error";
				} else if (List.class.isAssignableFrom(o)) {
					return "list";
				} else if (Collection.class.isAssignableFrom(o)) {
					return "set";
				} else if (RawData.class.isAssignableFrom(o)) {
					return "raw data";
				} else if (o.getName().startsWith("idawi.")) {
					return TextUtilities.f(o.getSimpleName());
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
