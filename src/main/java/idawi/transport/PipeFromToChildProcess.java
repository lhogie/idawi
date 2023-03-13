package idawi.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.messaging.Message;
import toools.io.Utilities;
import toools.io.Utilities.ReadUntilResult;
import toools.thread.Q;
import toools.thread.Threads;

public class PipeFromToChildProcess extends TransportService {
	public static class FirstResponse implements Serializable{
		public Object content;
	}
	
	public static class Entry {
		InputStream stdout, stderr;
		OutputStream stdin;
		public ComponentRef child;
		boolean run;
		public Q<FirstResponse> waitForChild = new Q<>(1);
		public long base64Len;
	}

	// the mark that announces a binary message coming from child stdout
	public static final String base64ObjectMark = "--- BASE64 OBJECT --> ";

	private final Map<ComponentRef, Entry> child_entry = new HashMap<>();

	public PipeFromToChildProcess(Component c) {
		super(c);
	}

	public Entry add(ComponentRef child, Process p) throws IOException {
		var e = new Entry();
		e.stdout = p.getInputStream();
		e.stderr = p.getErrorStream();
		e.stdin = p.getOutputStream();
		e.child = child;
		e.run = true;

		Threads.newThread_loop(() -> e.run, () -> processChildStandardStream(e, e.stdout, System.out));
		Threads.newThread_loop(() -> e.run, () -> processChildStandardStream(e, e.stderr, System.err));

		child_entry.put(child, e);
		return e;
	}

	@Override
	public Set<ComponentRef> actualNeighbors() {
		return child_entry.keySet();
	}

	static int nbW = 0;

	@Override
	protected void multicastImpl(Message msg, Collection<ComponentRef> neighbors) {
		for (var n : neighbors) {
			var e = child_entry.get(n);

			if (e == null)
				throw new IllegalStateException("can't send to " + n);

			transport(msg, e);
		}
	}

	@Override
	protected void bcastImpl(Message msg) {
		child_entry.values().forEach(e -> transport(msg, e));
	}

	private void transport(Message msg, Entry e) {
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
	public boolean canContact(ComponentRef c) {
		return child_entry.containsKey(c);
	}

	private void processChildStandardStream(Entry e, InputStream from, PrintStream to) {
		try {
			ReadUntilResult readResult = Utilities.readUntil(from, (byte) '\n');
			var line = new String(readResult.bytes.toByteArray());

			if (line.startsWith(base64ObjectMark)) {
				var base64 = line.substring(base64ObjectMark.length()); // get the rest of the line
				e.base64Len += base64.length();
				var bytes = Base64.getDecoder().decode(base64); // decode base64
				var o = serializer.fromBytes(bytes); // convert to message

				if (o instanceof Message) {
					processIncomingMessage((Message) o);
				} else {
					var f = new FirstResponse();
					f.content = o;
					e.waitForChild.add_sync(f);
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
}
