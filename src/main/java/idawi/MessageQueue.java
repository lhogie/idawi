package idawi;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import idawi.Streams.Chunk;
import toools.io.Cout;
import toools.thread.Q;
import toools.util.Date;

public class MessageQueue extends Q<Message> {
	public final Service service;
	public final String name;
//	private final Consumer<MessageQueue> destructor;
//	private final Set<ComponentDescriptor> completedSenders = new HashSet<>();

	private double expirationDate = Date.time() + DEFAULT_LIFETIME;
	public static double DEFAULT_LIFETIME = 60;

	private double maxWaitTimeS = DEFAULT_TIMEOUT_IN_SECONDS;
	public static double DEFAULT_TIMEOUT_IN_SECONDS = 1;

	public enum Enough {
		yes, no;
	}

	public MessageQueue(Service service, String name, int capacity) {
		super(capacity);
		this.name = name;
		this.service = service;
	}

	public double getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(double newExpirationDate) {
		if (newExpirationDate < Date.time())
			throw new IllegalArgumentException("expiration date is set in the past");

		this.expirationDate = newExpirationDate;
	}

	public void expandExpirationDate(double expansion) {
		setExpirationDate(getExpirationDate() + expansion);
	}

	public MessageQueue setMaxWaitTimeS(double maxWaitTimeS) {
		this.maxWaitTimeS = maxWaitTimeS;
		return this;
	}

	public enum Timeout {
		NO, YES
	}

	// sends a message and waits for returns, until timeout, EOT or explicit req by
	// the returnsHandler
	public Enough forEach(Function<Message, Enough> returnsHandler) {
		while (true) {
			double remains = expirationDate - Date.time();

			// queue has expired
			if (remains <= 0) {
				service.deleteQueue(this);
				return Enough.no;
			}

			double waitTime = Math.min(remains, maxWaitTimeS);
			Message msg = get_blocking(waitTime);
			Cout.debug("received " + msg);
			// no message arrived until the q dies or timeout expires
			if (msg == null) {
				service.deleteQueue(this);
				return Enough.no;
			} else if (returnsHandler.apply(msg) == Enough.yes) {
				service.deleteQueue(this);
				return Enough.yes;
			}
		}
	}

	public Enough forEach2(BiFunction<Message, List<Message>, Enough> c) {
		final var l = new ArrayList<Message>();

		return forEach(msg -> {
			var r = c.apply(msg, l);
			l.add(msg);
			return r;
		});
	}

	public Enough forEachUntilEOF(Consumer<Message> c) {
		return forEach(msg -> {
			if (msg.isEOT()) {
				return Enough.yes;
			} else {
				c.accept(msg);
				return Enough.no;
			}
		});
	}

	// sends a message and waits for returns, until EOT or the returnsHandler asks
	// to stop waiting
	// incoming messages are demultiplexed according to their role
	public Enough forEach(Function<Message, Enough> resultHandler, Function<Message, Enough> errorHandler,
			Function<Message, Enough> progressHandler, Function<Message, Enough> completionHandler) {
		return forEach(r -> {
			if (r.isEOT()) {
				return completionHandler.apply(r);
			} else if (r.isError()) {
				return errorHandler.apply(r);
			} else if (r.isProgress()) {
				return progressHandler.apply(r);
			} else {
				return resultHandler.apply(r);
			}
		});
	}

	public Object get() throws Throwable {
		return collectUntilFirstEOT().throwAnyError().resultMessages(1).first().content;
	}

	/**
	 * Collects timeout expires.
	 * 
	 * @return
	 */
	public MessageList collect() {
		return collect(msg -> Enough.no);
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
			return senders.equals(s) ? Enough.yes : Enough.yes;
		});
	}

	public MessageList collectUntilNEOT(int n) {
		AtomicInteger nbEOT = new AtomicInteger();
		return collect(msg -> msg.isEOT() && nbEOT.incrementAndGet() == n ? Enough.yes : Enough.no);
	}

	public MessageList collect(Function<Message, Enough> returnsHandler) {
		MessageList l = new MessageList();
		l.enough = forEach(msg -> {
			// if (msg.content instanceof Throwable && !(msg.content instanceof
			// RemoteException))
			// Cout.debugSuperVisible(msg.content);

			l.add(msg);
			return returnsHandler.apply(msg);
		});

		return l;
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
