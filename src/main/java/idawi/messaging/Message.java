package idawi.messaging;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.RemoteException;
import idawi.routing.QueueAddress;
import idawi.routing.Route;
import idawi.transport.Vault;
import toools.Objeects;
import toools.SizeOf;
import toools.io.ser.Serializer;
import toools.text.TextUtilities;

public class Message implements Serializable, SizeOf {
	private static final long serialVersionUID = 1L;

	public long ID = ThreadLocalRandom.current().nextLong();
	public Route route = new Route();

	public QueueAddress qAddr;
	public boolean autoStartService = false;
	public boolean alertServiceNotAvailable = false;
	public Object content;
	public RoutingStrategy initialRoutingStrategy;
	public boolean eot = false;
//	public ExecReq exec;

	public String contentDescription;

	public boolean autoCreateQueue = false;

	public boolean simulate = false;

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
		s += ", qAddr:" + qAddr;
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
		return 8 + SizeOf.sizeOf(initialRoutingStrategy) + route.sizeOf() + SizeOf.sizeOf(content);
	}

	public Object decrypt() {
		var c = content;
		int i = route.len();
		var ser = route.getLast().link.dest.serializer;

		while (c instanceof Vault) {
			var encryptper = route.get(i--).link.src.component;
			c = ser.fromBytes(((Vault) c).decode(encryptper.publicKey()));
		}

		return c;
	}

	public ExecReq exec() {
		return (ExecReq) content;
	}

}
