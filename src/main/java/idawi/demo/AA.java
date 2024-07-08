package idawi.demo;

import java.io.Serializable;
import java.util.HashSet;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.RemoteException;
import idawi.messaging.ACK;
import idawi.messaging.Message;
import idawi.messaging.ProgressInformation;
import idawi.messaging.ProgressMessage;
import idawi.messaging.ProgressRatio;
import idawi.messaging.RoutingStrategy;
import idawi.routing.QueueAddress;
import idawi.routing.Route;
import idawi.transport.Vault;
import toools.Objeects;
import toools.SizeOf;
import toools.io.ser.Serializer;

public record AA(long ID, Route route, QueueAddress qAddr, boolean autoStartService, Object content,
		RoutingStrategy initialRoutingStrategy, boolean eot, Class<? extends InnerClassEndpoint> endpointID,
		int nbSpecificThreads, QueueAddress replyTo, long soonestExecTime, long latestExecTime,
		boolean deleteQueueAfterCompletion, boolean autoCreateQueue, boolean simulate,
		HashSet<Class<? extends ACK>> ackReqs) implements Serializable, SizeOf {
	
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
		return 8 + SizeOf.sizeOf(initialRoutingStrategy) + route.sizeOf() + SizeOf.sizeOf(content) + qAddr.sizeOf();
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

	public AA throwIfError() {
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