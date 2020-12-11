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
	public To to;
	public To replyTo;
	public Object content;
	public double emissionDate, receptionDate;
	public boolean dropIfRecipientQueueIsFull = false;

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Message)) {
			return false;
		}

		Message m = (Message) o;

		return ID == m.ID && Utils.equals(route, m.route) && Utils.equals(to, m.to) && Utils.equals(content, m.content)
				&& emissionDate == m.emissionDate && receptionDate == m.receptionDate;
	}

	@Override
	public int hashCode() {
		throw new IllegalStateException("32-bit int hash code is not precise enough. Use the ID instead");
	}

	public double expirationDate() {
		return emissionDate + to.validityDuration;
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

		if (replyTo != null) {
			s += ", return:" + replyTo;
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
		out.writeObject(replyTo);
		out.writeObject(content);
		out.writeDouble(emissionDate);
		out.writeBoolean(dropIfRecipientQueueIsFull);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		ID = in.readLong();
		route = (Route) in.readObject();
		suggestedRoute = (Route) in.readObject();
		to = (To) in.readObject();
		replyTo = (To) in.readObject();
		content = in.readObject();
		emissionDate = in.readDouble();
		dropIfRecipientQueueIsFull = in.readBoolean();
	}

	public boolean isError() {
		return content instanceof MessageException;
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
}
