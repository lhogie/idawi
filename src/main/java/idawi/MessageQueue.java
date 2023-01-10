package idawi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import idawi.Streams.Chunk;
import toools.thread.Q;

public class MessageQueue extends Q<Message> {
	public final Service service;
	public final String name;
//	private final Consumer<MessageQueue> destructor;
//	private final Set<ComponentDescriptor> completedSenders = new HashSet<>();

	public MessageQueue(Service service, String name, int capacity) {
		super(capacity);
		this.name = name;
		this.service = service;
	}

	public void detach() {
		service.detachQueue(this);
	}

	public Object collectOneResult() throws Throwable {
		return collectUntilFirstEOT(1).messages.throwAnyError().resultMessages(1).first().content;
	}

	public <A> A collectOneResult(Class<A> c) throws Throwable {
		return (A) collectOneResult();
	}

	public MessageCollector collect(final double initialDuration, final double initialTimeout,
			Consumer<MessageCollector> collector) {
		var c = new MessageCollector(this);
		c.collect(initialDuration, initialTimeout, collector);
		return c;
	}

	public MessageCollector collect(Consumer<MessageCollector> collector) {
		return collect(1, 1, collector);
	}

	public MessageCollector recv_sync() {
		return recv_sync(1);
	}

	public MessageCollector recv_sync(final double initialDuration) {
		return collect(initialDuration, initialDuration, c -> {
		});
	}

	public MessageCollector collectUntilAllHaveReplied(final double initialDuration,
			Set<ComponentDescriptor> components) {
		return collect(initialDuration, initialDuration, c -> {
			c.stop = c.messages.senders().equals(components);
		});
	}

	/**
	 * Collects until one component have completed.
	 * 
	 * @return
	 */
	public MessageCollector collectUntilFirstEOT(double timeout) {
		return collect(timeout, timeout, c -> c.stop = c.messages.last().isEOT());
	}

	public MessageCollector collectUntilNEOT(double timeout, int n) {
		return collect(timeout, timeout, c -> c.stop = c.messages.countEOT() == n);
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
				Message msg = poll_sync(timeout);

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

	@Override
	public MessageList toList() {
		var r = new MessageList();
		r.addAll(super.toList());
		return r;
	}
}
