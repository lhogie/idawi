package idawi;

import java.io.Serializable;
import java.security.PublicKey;

import idawi.transport.Link;
import idawi.transport.TransportService;
import toools.io.ser.JavaSerializer;

class IdawiSerializer extends JavaSerializer {

	static class ComponentRepresentative implements Serializable {
		PublicKey key;
		String friendlyName;
	}

	static class LinkRepresentative implements Serializable {
		Component srcC;
		Class<? extends TransportService> srcT;
		Component destC;
		Class<? extends TransportService> destT;
	}

	private final Component component;

	IdawiSerializer(Component c) {
		this.component = c;
	}

	@Override
	protected Object replaceAtDeserialization(Object o) {
		if (o instanceof ComponentRepresentative) {
			var r = ((ComponentRepresentative) o);
			var key = r.key;
			var c = component.localView().g.findComponentByPublicKey(key);

			if (c == null) {
				c = new Component(key);
				c.turnToDigitalTwin(component);
				component.localView().g.ensureExists(c);
			}

			c.friendlyName = r.friendlyName;
			return c;
		} else if (o instanceof LinkRepresentative) {
			var r = (LinkRepresentative) o;
			var src = r.srcC.service(r.srcT, true);
			var dest = r.destC.service(r.destT, true);
			var l = component.localView().g.findALinkConnecting(src, dest);
			return l != null ? l : new Link(src, dest);
		} else {
			return o;
		}
	}

	@Override
	protected Object replaceAtSerialization(Object o) {
		if (o instanceof Component) {
			var c = ((Component) o);
			var cr = new ComponentRepresentative();
			cr.key = c.publicKey();
			cr.friendlyName = c.friendlyName;
			return cr;
		} else if (o instanceof Link) {
			var l = (Link) o;
			var r = new LinkRepresentative();
			r.srcC = l.src.component;
			r.srcT = l.src.getClass();
			r.destC = l.dest.component;
			r.destT = l.dest.getClass();
			return r;
		} else {
			return super.replaceAtSerialization(o);
		}
	}
}