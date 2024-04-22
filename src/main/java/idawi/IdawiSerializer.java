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
			} else if (friendlyName != null && c.friendlyName != null) {
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

	public final TransportService transportService;

	public IdawiSerializer(TransportService c) {
		this.transportService = c;
	}

	@Override
	protected Object replaceAtSerialization(Object o) {
		if (o instanceof Component c) {
			return c.representative();
		} else if (o instanceof Link l) {
			return l.toBeResolved ? new SourceRepresentative(l.src) : new LinkRepresentative(l.src, l.dest);
		} else {
			return super.replaceAtSerialization(o);
		}
	}

	@Override
	protected Object replaceAtDeserialization(Object o) {
		if (o instanceof ComponentRepresentative r) {
			var twin = transportService.component.localView().g.findComponent(d -> r.matches(d), true, () -> {
				var newComponent = new Component();
				newComponent.turnToDigitalTwin(transportService.component);
				return newComponent;
			});

			twin.keyPair = new KeyPair(r.key, null);
			twin.friendlyName = r.friendlyName; // may have changed
			// Cout.debugSuperVisible(r.friendlyName + " -> " + twin);
			return twin;
		} else if (o instanceof SourceRepresentative representative) {
			var src = representative.srcC.need(representative.srcT);
//			Cout.debug(src + "     ->    " + transportService);
			var l = transportService.component.localView().g.findLink(src, transportService, true, null);
			return l;
		} else if (o instanceof LinkRepresentative representative) {
			var src = representative.srcC.need(representative.srcT);
			var dest = representative.destC.need(representative.destT);
			return transportService.component.localView().g.findLink(src, dest, true, null);
		} else {
			return o;
		}
	}

}