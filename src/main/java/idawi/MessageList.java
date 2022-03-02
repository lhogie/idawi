package idawi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import idawi.MessageQueue.Enough;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import toools.exceptions.CodeShouldNotHaveBeenReachedException;

public class MessageList extends ArrayList<Message> {
	private static final long serialVersionUID = 1L;
	//public Enough enough;

	public DoubleList receptionDates() {
		DoubleList receptionDates = new DoubleArrayList();

		for (var m : this) {
			receptionDates.add(m.receptionDate);
		}

		return receptionDates;
	}

	public void clearContents() {
		forEach(m -> m.content = null);
	}

	public Set<ComponentDescriptor> completedPeers() {
		var r = new HashSet<ComponentDescriptor>();

		for (var m : this) {
			if (m.isEOT()) {
				r.add(m.route.source().component);
			}
		}

		return r;
	}

	public Set<ComponentDescriptor> uncompletedPeers() {
		var r = new HashSet<ComponentDescriptor>();

		for (var m : this) {
			if (m.isEOT()) {
				r.remove(m.route.source().component);
			} else {
				r.add(m.route.source().component);
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
		var firstCompleted = filter(msg -> msg.isEOT()).ensureSize(1).first().route.source().component;
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

		if (results.size() < n)
			throw new IllegalStateException("not enough results");

		return results;
	}

	public MessageList ensureSize(int n) {
		if (size() < n) 
			throw new IllegalStateException("not enough elements");

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

	public Map<ComponentDescriptor, MessageList> classifyByComponent() {
		Map<ComponentDescriptor, MessageList> r = new HashMap<>();

		for (Message m : this) {
			MessageList l = r.get(m.route.source().component);

			if (l == null) {
				r.put(m.route.source().component, l = new MessageList());
			}

			l.add(m);
		}

		return r;
	}

	public Map<String, MessageList> classifyByComponentName() {
		Map<String, MessageList> r = new HashMap<>();

		for (Message m : this) {
			MessageList l = r.get(m.route.source().component);

			if (l == null) {
				r.put(m.route.source().component.name, l = new MessageList());
			}

			l.add(m);
		}

		return r;
	}

	public MessageList throwAnyError() throws Throwable {
		for (var m : this) {
			if (m.isError()) {
				Throwable e = (RemoteException) m.content;

				while (e.getCause() != null) {
					e = e.getCause();
				}

				throw e;
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

	public Set<ComponentDescriptor> senders() {
		var r = new HashSet<ComponentDescriptor>();
		forEach(msg -> r.add(msg.route.source().component));
		return r;
	}

	public Set<Route> routes() {
		var r = new HashSet<Route>();
		forEach(msg -> r.add(msg.route));
		return r;
	}

	public boolean receptionTimeOrdered() {
		var len = size();

		for (int i = 1; i < len; ++i) {
			if (get(i - 1).receptionDate >= get(i).receptionDate) {
				return false;
			}
		}

		return true;
	}

	public double timeSpan() {
		return get(size() - 1).receptionDate - get(0).receptionDate;
	}

	public Set<ComponentDescriptor> components() {
		var r = new HashSet<ComponentDescriptor>();
		forEach(msg -> r.addAll(msg.route.components()));
		return r;
	}
}
