package idawi.service.web;

import java.util.Collection;

import idawi.Component;
import jaseto.ArrayNode;
import jaseto.DefaultCustomizer;
import jaseto.Node;
import jaseto.ObjectNode;

public class IdawiWebSerializer extends JasetoSerializer {
	public IdawiWebSerializer() {
		j.customizer = new DefaultCustomizer() {

			@Override
			public Object substitute(Object o) {
				if (o instanceof Component) {
					return o.toString();
				}

				return o;
			}

			@Override
			public Node alter(Node n) {
				if(!n.path().equals(".content")) {
					//n.removeKey("#class");
				}

				/*
				if (//n.value instanceof Collection)
				{
					var on = (ObjectNode) n;
					var elements = (ArrayNode)  on.map.remove("elements");
					elements.children.forEach(c -> on.map.put("", c));
				}*/
				
				return n;
			}
		};
	}
}
