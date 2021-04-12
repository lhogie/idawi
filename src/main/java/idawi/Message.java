package idawi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.ThreadLocalRandom;

import toools.text.TextUtilities;
import toools.util.Date;

public class Message implements Externalizable {
	private static final long serialVersionUID = 1L;

	public long ID = ThreadLocalRandom.current().nextLong();
	public Route route = new Route();
	Route suggestedRoute;
	public QueueAddress to;
	public QueueAddress requester;
	public Object content;
	public double receptionDate;
	public double creationDate = Date.time();
	public boolean dropIfRecipientQueueIsFull = false;
	public Object routingData;

	public Message() {
	}

	public Message(Object content, QueueAddress to, QueueAddress replyTo) {
		this.to = to;
		this.requester = replyTo;
		this.content = content;
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
		return creationDate + to.getValidityDuration();
	}

	public double remainingTime() {
		return expirationDate() - Date.time();
	}

	public boolean isExpired() {
		return remainingTime() <= 0;
	}

	@Override
	public String toString() {
		String s = "msg " + ID + ", route:" + route + " to:" + to;

		if (requester != null) {
			s += ", return:" + requester;
		}

		s += ", content: " + TextUtilities.toString(content);
		return s;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(ID);
		out.writeObject(route);
		out.writeObject(suggestedRoute);
		out.writeObject(to);
		out.writeObject(requester);
		out.writeObject(content);
		out.writeDouble(creationDate);
		out.writeBoolean(dropIfRecipientQueueIsFull);
		out.writeObject(routingData);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		ID = in.readLong();
		route = (Route) in.readObject();
		suggestedRoute = (Route) in.readObject();
		to = (QueueAddress) in.readObject();
		requester = (QueueAddress) in.readObject();
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
