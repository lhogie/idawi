package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ThreadLocalRandom;

import idawi.net.NetworkingService;
import toools.text.TextUtilities;
import toools.util.Date;

public class Message implements Externalizable {
	private static final long serialVersionUID = 1L;

	public long ID = ThreadLocalRandom.current().nextLong();
	public Route route = new Route();
	Route suggestedRoute;
	public QueueAddress to;
	public QueueAddress replyTo;
	public Object content;
	public double receptionDate;
	public double creationDate = Date.time();
	public boolean dropIfRecipientQueueIsFull = false;
	public Object routingData;

	public Class<? extends Service> originService;

	public Message() {
	}

	public Message(Object content, QueueAddress to, QueueAddress replyTo) {
		this.to = to;
		this.replyTo = replyTo;
		this.content = content;
	}

	public void send(Component fromComponent) {
//		if (to.notYetReachedExplicitRecipients != null && to.notYetReachedExplicitRecipients.contains(fromComponent.descriptor())) {
//			route.add(fromComponent.descriptor());
//			fromComponent.lookupService(NetworkingService.class).messagesFromNetwork.accept(this);
//		}

		fromComponent.lookup(NetworkingService.class).send(this);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Message) {
			Message m = (Message) o;
			return ID == m.ID && Utils.equals(route, m.route) && Utils.equals(to, m.to)
					&& Utils.equals(content, m.content) && receptionDate == m.receptionDate;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		throw new IllegalStateException("32-bit int hash code is not precise enough. Use the ID instead");
	}

	public double expirationDate() {
		return creationDate + to.serviceAddress.to.getValidityDuration();
	}

	public double remainingTime() {
		return expirationDate() - Date.time();
	}

	public boolean isExpired() {
		return remainingTime() <= 0;
	}

	@Override
	public String toString() {
		String s = "msg " + Long.toHexString(ID);

		if (originService != null) {
			s += ", origin=" + originService;
		}

		s += ", route:" + route + " to:" + to;

		if (replyTo != null) {
			s += ", return:" + replyTo;
		}

		s += ", content: " + TextUtilities.toString(content);
		return s;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(ID);
		out.writeObject(originService);
		out.writeObject(route);
		out.writeObject(suggestedRoute);
		out.writeObject(to);
		out.writeObject(replyTo);
		out.writeObject(content);
		out.writeDouble(creationDate);
		out.writeBoolean(dropIfRecipientQueueIsFull);
		out.writeObject(routingData);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		ID = in.readLong();
		this.originService = (Class<? extends Service>) in.readObject();
		route = (Route) in.readObject();
		suggestedRoute = (Route) in.readObject();
		to = (QueueAddress) in.readObject();
		replyTo = (QueueAddress) in.readObject();
		content = in.readObject();
		creationDate = in.readDouble();
		dropIfRecipientQueueIsFull = in.readBoolean();
		routingData = in.readObject();
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

	public double transportDuration() {
		return receptionDate - route.last().emissionDate;
	}
}
