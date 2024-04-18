package idawi.messaging;

import java.io.Serializable;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import idawi.Component;
import idawi.Endpoint;
import idawi.RemoteException;
import idawi.routing.QueueAddress;
import idawi.routing.Route;
import idawi.transport.Vault;
import toools.Objeects;
import toools.SizeOf;
import toools.io.ser.Serializer;

public class Message<C> implements Serializable, SizeOf {
	private static final long serialVersionUID = 1L;

	public final long ID = ThreadLocalRandom.current().nextLong();
	public final Route route = new Route();

	public QueueAddress qAddr;
	public QueueAddress replyTo;
	public boolean autoStartService = false;
	public C content = null;
	public Vault vault;
	public RoutingStrategy routingStrategy;
	public boolean eot = false;
	public Class<? extends Endpoint> endpointID;

	public int nbSpecificThreads = 1; // 0 makes it synchronous
	public Runtimes runtimes = new Runtimes();
	public boolean autoCreateQueue = true;
	public boolean deleteQueueAfterCompletion = false;

	public boolean simulate = false;
	public HashSet<Class<? extends ACK>> ackReqs = null;

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
		s += ", route:" + route;
		s += ", qAddr:" + qAddr;
		s += ", " + endpointID.getSimpleName() + "(" + content + ")";
		s += ackReqs != null ? "ACK:" + ackReqs : "";
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
		return 8 + SizeOf.sizeOf(routingStrategy) + route.sizeOf() + SizeOf.sizeOf(content) + qAddr.sizeOf()
				+ runtimes.sizeOf();
	}

	public Object decrypt() {
		int i = route.len();
		var ser = route.getLast().link.dest.serializer;

		Object v = vault;

		while (v instanceof Vault vv) {
			var encryptper = route.get(i--).link.src.component;
			v = ser.fromBytes(vv.decode(encryptper.publicKey()));
		}

		return v;
	}

	public Message throwIfError() {
		if (content instanceof Throwable) {
			Throwable e = (RemoteException) content;

			while (e.getCause() != null) {
				e = e.getCause();
			}

			throw e instanceof RuntimeException re ? re : new RuntimeException(e);
		}

		return this;
	}
}
