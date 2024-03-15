package idawi.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import idawi.Service;
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


	public MessageCollector collector() {
		return new MessageCollector(this);
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

	@Override
	public MessageList toList() {
		var r = new MessageList();
		r.addAll(super.toList());
		return r;
	}
}
