package idawi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import toools.exceptions.CodeShouldNotHaveBeenReachedException;

public class MessageList extends ArrayList<Message> {

	private static final long serialVersionUID = 1L;
	public boolean timeout;

	public MessageList filter(Predicate<Message> p) {
		MessageList l = new MessageList();

		for (Message m : this) {
			if (p.test(m)) {
				l.add(m);
			}
		}

		return l;
	}

	public MessageList retainFirstCompleted() {
		ComponentInfo firstCompleted = filter(msg -> msg.isEOT()).ensureSize(1).first().route.source().component;
		MessageList l = new MessageList();

		for (Message m : this) {
			if (m.route.source().component.equals(firstCompleted)) {
				l.add(m);

				if (m.isEOT()) {
					return l;
				}
			}
		}

		throw new CodeShouldNotHaveBeenReachedException();
	}

	public Message first() {
		return get(0);
	}

	public Message last() {
		return get(size() - 1);
	}

	public Message getOrNull(int i) {
		return i < size() ? get(i) : null;
	}

	public Object getContentOrNull(int i) {
		return i < size() ? get(i).content : null;
	}

	public MessageList resultMessages() {
		return filter(m -> m.isResult());
	}

	public MessageList resultMessages(int n) {
		MessageList results = resultMessages();

		if (results.size() < n) {
			throw new IllegalStateException("not enough results");
		}

		return results;
	}

	public MessageList ensureSize(int n) {

		if (size() < n) {
			throw new IllegalStateException("not enough elements");
		}

		return this;
	}

	public List<Object> contents() {
		return stream().map(m -> m.content).collect(Collectors.toList());
	}

	public MessageList errorMessages() {
		return filter(m -> m.isResult());
	}

	public List<Throwable> errors() {
		return errorMessages().stream().map(new Function<Message, Throwable>() {

			@Override
			public Throwable apply(Message t) {
				return (Throwable) t.content;
			}
		}).collect(Collectors.toList());
	}

	public Map<ComponentInfo, MessageList> classifyByComponent() {
		Map<ComponentInfo, MessageList> r = new HashMap<>();

		for (Message m : this) {
			MessageList l = r.get(m.route.source().component);

			if (l == null) {
				r.put(m.route.source().component, l = new MessageList());
			}

			l.add(m);
		}

		return r;
	}

	public MessageList throwAnyError() throws MessageException {
		if (timeout)
			throw new MessageException("timeout");

		for (var m : this) {
			if (m.isError()) {
				throw (MessageException) m.content;
			}
		}

		return this;
	}

}
