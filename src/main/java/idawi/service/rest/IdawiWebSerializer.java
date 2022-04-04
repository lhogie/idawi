package idawi.service.rest;

import idawi.EOT;
import idawi.Message;
import jaseto.DefaultCustomizer;
import jaseto.ObjectNode;

public class IdawiWebSerializer extends JasetoSerializer {
	public IdawiWebSerializer() {
		j.customizer = new DefaultCustomizer() {

			@Override
			public Object substitute(Object o) {
				return o;
			}

			@Override
			public void alter(ObjectNode n) {
//				n.removeKey("#class");
			}
		};
	}
}
