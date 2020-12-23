package idawi.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import idawi.ComponentInfo;
import idawi.Message;
import idawi.TransportLayer;
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
	public final ComponentInfo child;

	// the mark that announces a binary message coming from child stdout
	public static final String msgMark = "dfmskfjqmkrgjhqsljkvbn<jksfh";

	public static interface EOFFound {
		void found();
	}

	private boolean run = false;

	public PipeFromToChildProcess(ComponentInfo child, Process p) throws IOException {
		this.stdout = p.getInputStream();
		this.stderr = p.getErrorStream();
		this.stdin = p.getOutputStream();
		this.child = child;
	}

	@Override
	public Set<ComponentInfo> neighbors() {
		return Collections.singleton(child);
	}

	@Override
	public void send(Message msg, Collection<ComponentInfo> neighbors) {
		if (!run)
			return;

		try {
			serializer.write(msg, stdin);
			stdin.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getName() {
		return "SSChild";
	}

	@Override
	public boolean canContact(ComponentInfo c) {
		return c.equals(child);
	}

	@Override
	public void injectLocalInfoTo(ComponentInfo c) {
	}

	@Override
	protected void start() {
		run = true;

		Threads.newThread_loop(() -> run, () -> {
			processNextLine(stdout, Cout.out);
		});

		Threads.newThread_loop(() -> run, () -> {
			processNextLine(stderr, Cout.err);
		});
	}

	public static final String started = "fmdskfjsfgkhjs STARTED";
	public static final String failed = "fmdskfjsfgkhjs FAILED";
	public Q<Object> waitForChild = new Q<>(1);

	private void processNextLine(InputStream in, Cout ps) {
		try {
			ReadUntilResult l = Utilities.readUntil(in, (byte) '\n');
			String line = new String(l.bytes.toByteArray());

			if (l.eof) {
				if (!line.isEmpty()) {
					ps.add(child + "> " + line);
				}

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
					Message msg = (Message) serializer.read(in);
					processIncomingMessage(msg);
				} catch (IOException e) {
					// the binary input stream was dirty
					// e.printStackTrace();
				}
			} else if (TextUtilities.isASCIIPrintable(line)) {
				ps.add(child + "> " + line);
			} else {
				ps.add(child + "> *** non-printable data ***: " + line);
			}
		} catch (Exception e) {
			run = false;
		}
	}

	@Override
	protected void stop() {
		run = false;
	}
}
