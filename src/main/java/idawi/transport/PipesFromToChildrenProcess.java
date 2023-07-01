package idawi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import idawi.Component;
import idawi.messaging.Message;
import toools.io.Utilities;
import toools.io.Utilities.ReadUntilResult;
import toools.thread.Q;
import toools.thread.Threads;

public class PipesFromToChildrenProcess extends TransportService {

	public static class Entry {
		InputStream stdout, stderr;
		OutputStream stdin;
		public Component child;
		boolean run;
		public Q waitForChild = new Q<>(1);
		public long base64Len;
		public Process process;
	}

	// the mark that announces a binary message coming from child stdout
	public static final String base64ObjectMark = "--- BASE64 OBJECT --> ";

	private final Map<Component, Entry> child_entry = new HashMap<>();

	public PipesFromToChildrenProcess(Component c) {
		super(c);
	}

	public Entry add(Component child, Process p) throws IOException {
		var e = new Entry();
		e.stdout = p.getInputStream();
		e.stderr = p.getErrorStream();
		e.stdin = p.getOutputStream();
		e.child = child;
		e.run = true;
		e.process = p;

		Threads.newThread_loop(() -> e.run, () -> processChildStandardStream(e, e.stdout, System.out));
		Threads.newThread_loop(() -> e.run, () -> processChildStandardStream(e, e.stderr, System.err));

		child_entry.put(child, e);
		return e;
	}

	public Collection<Component> actualNeighbors() {
		return child_entry.keySet();
	}

	static int nbW = 0;

	@Override
	protected void sendImpl(Message msg) {
		var n = msg.route.last().link;
		var e = child_entry.get(n);

		if (e == null)
			throw new IllegalStateException("can't send to " + n);

		try {
			serializer.write(msg, e.stdin);
			e.stdin.flush();
		} catch (IOException err) {
			throw new RuntimeException(err);
		}
	}

	@Override
	public String getName() {
		return "pipe to child processes";
	}

	@Override
	public boolean canContact(Component c) {
		return child_entry.containsKey(c);
	}

	private void processChildStandardStream(Entry e, InputStream from, PrintStream to) {
		try {
			ReadUntilResult readResult = Utilities.readUntil(from, (byte) '\n');
			var line = new String(readResult.bytes.toByteArray());

			if (line.startsWith(base64ObjectMark)) {
				var base64 = line.substring(base64ObjectMark.length()); // get the rest of the line
				e.base64Len += base64.length();
				var bytes = Base64.getDecoder().decode(base64);
				var o = serializer.fromBytes(bytes);

				if (o instanceof Message) {
					processIncomingMessage((Message) o);
				} else {
					e.waitForChild.add_sync(o);
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
		long sum = 0;

		for (var e : child_entry.values()) {
			sum += e.base64Len;
		}

		return sum;
	}

	@Override
	public void dispose(Link l) {
		Entry e = child_entry.get(l.dest.component);
		e.process.destroy();

		Stream.of(e.stderr, e.stdin, e.stdout).forEach(s -> {
			try {
				s.close();
			} catch (IOException err) {
			}
		});

	}
}
