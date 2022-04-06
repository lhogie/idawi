package idawi;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import idawi.MessageQueue.Enough;
import toools.util.Date;

public class MessageIterator implements Iterator<Message> {
	private double during;
	private double waitTime;
	private Function<Message, Enough> f;
	Message next;
	boolean completed = false;
	final double endDate ;
	private MessageQueue q;

	public MessageList collect(Iterator<Message> i) {
		var l = new MessageList();
		i.forEachRemaining(msg -> l.add(msg));
		return l;
	}

	public MessageIterator(MessageQueue q, final double during, final double waitTime, Function<Message, Enough> f) {
		this.during = during;
		this.waitTime = waitTime;
		this.f = f;
		this.endDate  = Date.time() + during;
		this.q = q;
	}

	@Override
	public boolean hasNext() {
		if (completed)
			return false;

		if (next == null)
			advance();

		return next != null;
	}

	private void advance() {
		var remains = endDate - Date.time();

		if (remains <= 0) {
			// expired!
			q.detach();
			completed = true;
		} else {
			var w = Math.min(remains, waitTime);
			next = q.poll_sync(w);

			// no message arrived until the q expires
			if (next == null) {
				q.detach();
				completed = true;
			} else {
				if (f.apply(next) == Enough.yes) {
					q.detach();
					completed = true;
				} else {
					completed = false;
				}
			}
		}
	}

	@Override
	public Message next() {
		if (completed)
			throw new NoSuchElementException();

		if (next == null)
			advance();

		if (next == null)
			return next;

		throw new NoSuchElementException();
	}
}
