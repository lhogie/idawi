package idawi.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Message;
import toools.io.Cout;
import toools.io.Utilities;
import toools.io.Utilities.ReadUntilResult;
import toools.text.TextUtilities;
import toools.thread.Q;
import toools.thread.Threads;

public class PipeFromToChildProcess extends TransportLayer {
	final InputStream stdout, stderr;
	final OutputStream stdin;
	public EOFFound eofHandler;
	public final ComponentDescriptor child;

	// the mark that announces a binary message coming from child stdout
	public static final String msgMark = "--- MESSAGE MARK --- DZH98744SKO";

	public static interface EOFFound {
		void found();
	}

	private boolean run = false;
	private final Set<ComponentDescriptor> neighbors;

	public PipeFromToChildProcess(Component c, ComponentDescriptor child, Process p) throws IOException {
		super(c);
		this.stdout = p.getInputStream();
		this.stderr = p.getErrorStream();
		this.stdin = p.getOutputStream();
		this.child = child;
		this.neighbors = Collections.singleton(child);
	}

	@Override
	public Set<ComponentDescriptor> neighbors() {
		return neighbors;
	}

	static int nbW = 0;

	@Override
	public void send(Message msg, Collection<ComponentDescriptor> neighbors) {
		if (!run)
			return;

		try {
//			System.out.println("write(" + serializer.toBytes(msg).length + " bytes" + ")        " + nbW++);
//			Threads.sleep(0.5);
			serializer.write(msg, stdin);
//			System.out.println("flush()");
			stdin.flush();
//			System.out.println("flush ok");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getName() {
		return "pipe";
	}

	@Override
	public boolean canContact(ComponentDescriptor c) {
		return c.equals(child);
	}

	@Override
	public void injectLocalInfoTo(ComponentDescriptor c) {
	}

	@Override
	protected void start() {
		run = true;

		Threads.newThread_loop(() -> run, () -> {
			processNextLine(stdout, Cout.out, Cout.err);
		});

		Threads.newThread_loop(() -> run, () -> {
			processNextLine(stderr, Cout.err, Cout.err);
		});
	}

	public static final String started = "fmdskfjsfgkhjs STARTED";
	public static final String failed = "fmdskfjsfgkhjs FAILED";
	public Q<Object> waitForChild = new Q<>(1);

	private void processNextLine(InputStream in, Cout out, Cout err) {
		try {
			ReadUntilResult l = Utilities.readUntil(in, (byte) '\n');
			String line = new String(l.bytes.toByteArray());
//			System.out.println("stdoutc hild : " + line);
			if (l.eof) {
				if (!line.isEmpty()) {
					out.add(child + "> " + line);
				}

				out.add(child + "> END OF TRANMISSION");
				run = false;

				if (eofHandler != null) {
					eofHandler.found();
				}
			} else if (line.equals(started)) {
				waitForChild.add_blocking(started);
			} else if (line.equals(failed)) {
				waitForChild.add_blocking(failed);
			} else if (line.equals(msgMark)) {
				try {
					var msg = (Message) serializer.read(in);
					processIncomingMessage(msg);
				} catch (IOException e) {
					err.add("error decoding message from " + child + " (stream was dirty probably): "
							+ TextUtilities.exception2string(e));
					// e.printStackTrace();
				}
			} else if (TextUtilities.isASCIIPrintable(line)) {
				out.add(child + "> " + line);
			} else {
				err.add(child + "> *** non-printable data ***: " + line);
			}
		} catch (Exception e) {
			run = false;
			err.add(TextUtilities.exception2string(e));
		}
	}

	@Override
	protected void stop() {
		run = false;
	}
}
