package idawi.messaging;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.RemoteException;
import idawi.routing.Destination;
import idawi.routing.Route;
import toools.Objeects;
import toools.SizeOf;
import toools.io.ser.Serializer;
import toools.text.TextUtilities;

public class Message implements Serializable, SizeOf {
	private static final long serialVersionUID = 1L;

	public long ID = ThreadLocalRandom.current().nextLong();
	public Route route = new Route();

	// targeted to either a queue or an operation
	public Destination destination;
	public Object content;

	public final RoutingStrategy routingStrategy;

	public boolean eot = false;

	public boolean simulate = true;

	public boolean alertServiceNotAvailable = true;

	public Message(Destination dest, RoutingStrategy routingStrategy, Object content) {
		this.destination = dest;
		this.content = content;
		this.routingStrategy = routingStrategy;
	}

	public Message clone(Serializer ser) {
		return (Message) ser.clone(this);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Message) {
			Message m = (Message) o;
			return ID == m.ID && route.equals(m.route) && Objeects.equals(content, m.content);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		throw new IllegalStateException("32-bit int hash code is not precise enough. Use the 64-bit ID instead");
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
		return eot;
	}

	public boolean isResult() {
		return !isError() && !isProgress();
	}

	public Component sender() {
		return route.source();
	}

	@Override
	public long sizeOf() {
		return 8 + destination.sizeOf() + routingStrategy.sizeOf() + route.sizeOf() + SizeOf.sizeOf(content);
	}

}
