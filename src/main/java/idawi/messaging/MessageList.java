package idawi.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import idawi.RemoteException;
import idawi.knowledge_base.ComponentRef;
import idawi.routing.Route;

public class MessageList extends ArrayList<Message> {
	private static final long serialVersionUID = 1L;
	// public Enough enough;

	public void clearContents() {
		forEach(m -> m.content = null);
	}

	public MessageList eots() {
		return filter(m -> m.isEOT());
	}

	public Set<ComponentRef> completedPeers() {
		return eots().senders();
	}

	public Set<ComponentRef> uncompletedPeers() {
		var r = new HashSet<ComponentRef>();

		for (var m : this) {
			if (m.isEOT()) {
				r.remove(m.route.initialEmission().component);
			} else {
				r.add(m.route.initialEmission().component);
			}
		}

		return r;
	}

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
		var firstCompleted = eots().first().route.initialEmission().component;
		return filter(m -> m.route.initialEmission().component.equals(firstCompleted) && !m.isEOT());
	}

	public Message first() {
		return get(0);
	}

	public Message last() {
		return last(0);
	}

	public Message last(int rewind) {
		return get(size() - 1 - rewind);
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

		if (results.size() < n)
			throw new IllegalStateException("no " + n + " result(s) in this list");

		return results;
	}

	public MessageList ensureSize(int n) {
		if (size() < n)
			throw new IllegalStateException("no " + n + " element(s) in this list");

		return this;
	}

	public List<Object> contents() {
		return stream().map(m -> m.content).toList();
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
		}).toList();
	}

	public Map<ComponentRef, MessageList> sender2message() {
		Map<ComponentRef, MessageList> r = new HashMap<>();

		for (Message m : this) {
			MessageList l = r.get(m.route.initialEmission().component);

			if (l == null) {
				r.put(m.route.initialEmission().component, l = new MessageList());
			}

			l.add(m);
		}

		return r;
	}

	public Map<String, MessageList> senderName2messages() {
		Map<String, MessageList> r = new HashMap<>();

		for (var e : sender2message().entrySet()) {
			r.put(e.getKey().ref, e.getValue());
		}

		return r;
	}

	public Map<String, List<Object>> senderName2contents() {
		Map<String, List<Object>> r = new HashMap<>();

		for (var e : sender2message().entrySet()) {
			r.put(e.getKey().ref, e.getValue().contents());
		}

		return r;
	}

	public MessageList throwAnyError() throws RuntimeException {
		for (var m : this) {
			if (m.isError()) {
				Throwable e = (RemoteException) m.content;

				while (e.getCause() != null) {
					e = e.getCause();
				}

				throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
			}
		}

		return this;
	}

	public MessageList throwAnyError_Runtime() {
		try {
			return throwAnyError();
		} catch (Throwable e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	public int count(Predicate<Message> p) {
		int n = 0;

		for (var m : this) {
			if (p.test(m)) {
				++n;
			}
		}

		return n;
	}

	public int countErrors() {
		return count(m -> m.isError());
	}

	public int countEOT() {
		return count(m -> m.isEOT());
	}

	public int countProgress() {
		return count(m -> m.isProgress());
	}

	public Set<ComponentRef> senders() {
		var r = new HashSet<ComponentRef>();
		forEach(msg -> r.add(msg.route.initialEmission().component));
		return r;
	}

	public Set<Route> routes() {
		var r = new HashSet<Route>();
		forEach(msg -> r.add(msg.route));
		return r;
	}

	public Set<ComponentRef> components() {
		var r = new HashSet<ComponentRef>();
		forEach(msg -> msg.route.components().forEach(c -> r.add(c)));
		return r;
	}
}
