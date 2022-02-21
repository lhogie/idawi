package idawi;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import idawi.Streams.Chunk;
import toools.thread.Q;

public class MessageQueue extends Q<Message> {
	public final Service service;
	public final String name;
//	private final Consumer<MessageQueue> destructor;
//	private final Set<ComponentDescriptor> completedSenders = new HashSet<>();

	public enum Enough {
		yes, no;
	}

	public MessageQueue(Service service, String name, int capacity) {
		super(capacity);
		this.name = name;
		this.service = service;
	}

	public enum Timeout {
		NO, YES
	}

	public void delete() {
		service.deleteQueue(this);
	}

	public Object get() throws Throwable {
		return collectUntilFirstEOT().throwAnyError().resultMessages(1).first().content;
	}

	public <A> Object getAs(Class<A> c) throws Throwable {
		return (A) get();
	}

	public MessageCollector collect(final double initialDuration, final double initialTimeout,
			Consumer<MessageCollector> collector) {
		var c = new MessageCollector(this);
		c.collect(initialDuration, initialTimeout, collector);
		return c;
	}

	public Enough forEach(final double during, final double waitTime, Function<Message, Enough> returnsHandler) {
		return collect(during, waitTime, c -> {
			c.stop = returnsHandler.apply(c.messages.last()) == Enough.yes;
		}).stop ? Enough.yes : Enough.no;
	}

	public Enough forEach(final double during, Function<Message, Enough> returnsHandler) {
		return forEach(during, during, returnsHandler);
	}

	public Enough forEach(Function<Message, Enough> returnsHandler) {
		return forEach(MessageCollector.DEFAULT_COLLECT_DURATION, returnsHandler);
	}

	public Enough forEachUntil(Predicate<Message> c) {
		return forEach(msg -> {
			if (c.test(msg)) {
				return Enough.yes;
			} else {
				return Enough.no;
			}
		});
	}

	public Enough forEachUntilFirstEOF(Consumer<Message> c) {
		return forEachUntil(msg -> {
			boolean eot = msg.isEOT();

			if (!eot) {
				c.accept(msg);
			}

			return eot;
		});
	}

	// sends a message and waits for returns, until EOT or the returnsHandler asks
	// to stop waiting
	// incoming messages are demultiplexed according to their role
	public Enough forEach(final double during, final double waitTime, MessageHandler h) {
		return forEach(during, waitTime, r -> {
			if (r.isEOT()) {
				return h.newEOT(r);
			} else if (r.isError()) {
				return h.newError(r);
			} else if (r.isProgressMessage()) {
				return h.newProgressMessage(r);
			} else if (r.isProgressRatio()) {
				return h.newProgressRatio(r);
			} else {
				return h.newResult(r);
			}
		});
	}

	public enum Filter {
		keep, drop
	}

	public MessageList collect(Iterator<Message> i) {
		var l = new MessageList();
		i.forEachRemaining(msg -> l.add(msg));
		return l;
	}

	public MessageList collect() {
		return collect(MessageCollector.DEFAULT_COLLECT_DURATION);
	}

	public MessageList collect(double duration) {
		return collect(duration, duration, c -> {
		}).messages;
	}

	public MessageList collect(Function<Message, Enough> f) {
		return collect(MessageCollector.DEFAULT_COLLECT_DURATION, MessageCollector.DEFAULT_COLLECT_DURATION, c -> {
			c.stop = f.apply(c.messages.last()) == Enough.yes;
		}).messages;
	}

	/**
	 * Collects until one component have completed.
	 * 
	 * @return
	 */
	public MessageList collectUntilFirstEOT() {
		return collect(msg -> msg.isEOT() ? Enough.yes : Enough.no);
	}

	public MessageList collect(Set<ComponentDescriptor> s) {
		Set<ComponentDescriptor> senders = new HashSet<>();

		return collect(msg -> {
			senders.add(msg.route.source().component);
			return senders.equals(s) ? Enough.yes : Enough.no;
		});
	}

	public MessageList collectUntilNEOT(int n) {
		AtomicInteger nbEOT = new AtomicInteger();
		return collect(msg -> msg.isEOT() && nbEOT.incrementAndGet() == n ? Enough.yes : Enough.no);
	}

	public InputStream restream(double timeout, BooleanSupplier keepOn) {
		return new InputStream() {
			byte[] buf;
			int i = -1;
			long nbRead = 0;

			@Override
			public int read(byte b[], int off, int len) throws IOException {
				int n = 0;

				while (len > 0) {
					if (buf == null || i >= buf.length) {
						fillBuffer();
					}

					int l = Math.min(buf.length, len);
					System.arraycopy(buf, i, b, off, l);
					len -= l;
					off += l;
					n += l;
				}

				return n;
			}

			private void fillBuffer() throws IOException {
				Message msg = get_blocking(timeout);

				if (msg == null) {
					throw new IOException("timeout");
				}

				var chunk = (Chunk) msg.content;
				buf = (byte[]) msg.content;
				i = 0;
			}

			private final byte[] singleton = new byte[0];

			public int read() throws IOException {
				if (read(singleton, 0, 1) == 1) {
					return singleton[0];
				} else {
					return -1;
				}
			}
		};
	}

	public QueueAddress addr() {
		return service.component.getAddress().s(service.id).q(name);
	}
}
