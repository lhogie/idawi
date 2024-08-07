package idawi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.io.Cout;
import toools.io.Utilities;
import toools.io.Utilities.ReadUntilResult;
import toools.math.MathsUtilities;
import toools.thread.Q;
import toools.thread.Threads;

public class Pipe_ParentSide extends TransportService  implements Broadcastable{

	public static class Entry {
		InputStream stdout, stderr;
		OutputStream stdin;
		public Component child;
		boolean run;
		public Q waitForChild = new Q<>(1);
		public long base64Len;
		public Process process;

		public Component waitForChild() throws Throwable {
			var o = waitForChild.poll_sync();

			if (o instanceof Throwable err) {
				throw err;
			} else if (o instanceof Message m) {
				return child = m.sender();
			} else {
				throw new IllegalStateException("what to do with that? " + o);
			}
		}
	}

	// the mark that announces a binary message coming from child stdout
	public static final String base64ObjectMark = "--- BASE64 OBJECT --> ";

	private final Set<Entry> child_entry = new HashSet<>();

	public Pipe_ParentSide(Component c) {
		super(c);
	}

	public Entry add(Process p) throws IOException {
		var e = new Entry();
		e.stdout = p.getInputStream();
		e.stderr = p.getErrorStream();
		e.stdin = p.getOutputStream();
		e.run = true;
		e.process = p;

		Threads.newThread_loop(() -> e.run, () -> processSdtStreamFromChild(e, e.stdout, System.out));
		Threads.newThread_loop(() -> e.run, () -> processSdtStreamFromChild(e, e.stderr, System.err));

		child_entry.add(e);
		return e;
	}

	public Collection<Component> actualNeighbors() {
		return child_entry.stream().map(e -> e.child).toList();
	}

	private Entry findEntry(Component n) {
		for (var e : child_entry) {
			if (e.child == n) {
				return e;
			}
		}

		return null;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	private void processSdtStreamFromChild(Entry e, InputStream from, PrintStream to) {
		try {
			ReadUntilResult readResult = Utilities.readUntil(from, (byte) '\n');
			var line = new String(readResult.bytes.toByteArray());
			if (line.startsWith(base64ObjectMark)) {
				var base64 = line.substring(base64ObjectMark.length()); // get the rest of the line
				e.base64Len += base64.length();
				var bytes = Base64.getDecoder().decode(base64);
				var o = serializer.fromBytes(bytes);
//				Cout.debug(component +  " receives " + o);

				if (o instanceof Message msg) {
					processIncomingMessage(msg);

					if (e.child == null) {
						e.waitForChild.add_sync(o);
					}

					e.child = msg.route.getLast().link.src.component;
					Cout.debug("route: " + msg.route);
					Cout.debug("child: " + e.child);
				} else if (o instanceof Throwable err) {
					e.waitForChild.add_sync(err);
				} else {
					to.println(o.getClass().getName() + " object received: " + o);
				}
			} else {
				to.println(e.child + "> " + line);
			}

			e.run = !readResult.eof;
		} catch (Exception err) {
			e.run = false;
			err.printStackTrace();
		}
	}

	public long base64Len() {
		return child_entry.stream().mapToLong(e -> e.base64Len).sum();
	}

	@Override
	public void dispose(Link l) {
		Entry e = findEntry(l.dest.component);
		e.process.destroy();

		Stream.of(e.stderr, e.stdin, e.stdout).forEach(s -> {
			try {
				s.close();
			} catch (IOException err) {
			}
		});

	}

	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.000010, 0.000030, Idawi.prng);
	}

	@Override
	protected void multicast(byte[] msg, Collection<Link> outLinks) {
//		Cout.debug(this + " multicast: " + outLinks);
		for (var l : outLinks) {
			var n = l.dest.component;
			var e = findEntry(n);

			if (e == null)
				throw new IllegalStateException("can't send to " + n);

			send(msg, e);
		}
	}

	@Override
	public void bcast(byte[] msg) {
//		Cout.debug(this + " bcast: " + child_entry.stream().map(e -> e.child).toList());

		for (var e : child_entry) {
			send(msg, e);
		}
	}

	private void send(byte[] msg, Entry e) {
		try {
			e.stdin.write(msg);
			e.stdin.flush();
		} catch (IOException err) {
			throw new RuntimeException(err);
		}
	}

}
