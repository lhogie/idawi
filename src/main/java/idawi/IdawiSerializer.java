package idawi;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PublicKey;

import idawi.transport.Link;
import idawi.transport.TransportService;
import toools.io.ser.JavaSerializer;

public class IdawiSerializer extends JavaSerializer {

	public static class ComponentRepresentative implements Serializable {
		PublicKey key;
		String friendlyName;
		
		public boolean matches(Component c) {
			if (key != null && c.publicKey() != null) {
				return key.equals(c.publicKey());
			}
			else if (friendlyName != null && c.friendlyName != null) {
				return friendlyName.equals(c.friendlyName);
			}

			throw new IllegalArgumentException("can't compare");
		}
	}

	public static class SourceRepresentative implements Serializable {
		Component srcC;
		Class<? extends TransportService> srcT;

		public SourceRepresentative(TransportService t) {
			this.srcC = t.component;
			this.srcT = t.getClass();
		}
	}

	public static class LinkRepresentative extends SourceRepresentative {
		Component destC;
		Class<? extends TransportService> destT;

		public LinkRepresentative(TransportService src, TransportService to) {
			super(src);
			this.destC = to.component;
			this.destT = to.getClass();
		}
	}

	private final TransportService ts;

	public IdawiSerializer(TransportService c) {
		this.ts = c;
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

			if (l.toBeResolved) {
				return new SourceRepresentative(l.src);
			} else {
				return new LinkRepresentative(l.src, l.dest);
			}
		} else {
			return super.replaceAtSerialization(o);
		}
	}

	@Override
	protected Object replaceAtDeserialization(Object o) {
		if (o instanceof ComponentRepresentative) {
			var r = ((ComponentRepresentative) o);
			var c = ts.component.localView().g.findComponent(d -> r.matches(d), true, d -> {
				d.keyPair = new KeyPair(r.key, null);
				d.turnToDigitalTwin(ts.component);
			});

			c.friendlyName = r.friendlyName; // may have changed
			return c;
		} else if (o instanceof SourceRepresentative) {
			var representative = (SourceRepresentative) o;
			var src = representative.srcC.service(representative.srcT, true);
			var l = ts.component.localView().g.findALinkConnecting(src, ts);
			return l != null ? l : new Link(src, ts);
		} else if (o instanceof LinkRepresentative) {
			var representative = (LinkRepresentative) o;
			var src = representative.srcC.service(representative.srcT, true);
			var dest = representative.destC.service(representative.destT, true);
			var l = ts.component.localView().g.findALinkConnecting(src, dest);
			return l != null ? l : new Link(src, dest);
		} else {
			return o;
		}
	}

}