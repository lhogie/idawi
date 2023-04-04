package idawi.messaging;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.RemoteException;
import idawi.Utils;
import idawi.routing.Destination;
import idawi.routing.Route;
import idawi.routing.RoutingData;
import toools.io.ser.JavaSerializer;
import toools.text.TextUtilities;

public class Message implements Serializable {
	private static final long serialVersionUID = 1L;

	public long ID = ThreadLocalRandom.current().nextLong();
	public Route route = new Route();

	// targeted to either a queue or an operation
	public Destination destination;
	public Object content;

	public Message(Destination dest, Object value) {
		if (dest == null)
			throw new NullPointerException();

		this.destination = dest;
		this.content = value;
	}

	private static final JavaSerializer ser = new JavaSerializer<>();

	public Message clone() {
		return (Message) ser.clone(this);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Message) {
			Message m = (Message) o;
			return ID == m.ID && route.equals(m.route) && Utils.equals(content, m.content);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		throw new IllegalStateException("32-bit int hash code is not precise enough. Use the 64-bit ID instead");
	}

	public RoutingData currentRoutingParameters() {
		return route.last().routingParms();
	}

	@Override
	public String toString() {
		String s = "msg " + Long.toHexString(ID);
		s += ", dest:" + destination;
		s += ", route:" + route;
		s += ", content: " + TextUtilities.toString(content);
		return s;
	}

	public boolean isError() {
		return content instanceof RemoteException;
	}

	public boolean isProgress() {
		return content instanceof ProgressInformation;
	}

	public boolean isProgressMessage() {
		return content instanceof ProgressMessage;
	}

	public boolean isProgressRatio() {
		return content instanceof ProgressRatio;
	}

	public boolean isEOT() {
		return content instanceof EOT;
	}

	public boolean isResult() {
		return !isError() && !isProgress() && !isEOT();
	}

	public Component sender() {
		return route.initialEmission().transport.component;
	}

}