package idawi.transport.serial;

import java.io.IOException;
import java.io.InputStream;

import toools.thread.Q;

public class InputStream2Q implements Runnable {
	private final Ok okSingleton = new Ok();
	public final Q q = new Q(1);
	private final InputStream in;
	
	public InputStream2Q(InputStream in) {
		this.in = in;
	}

	@Override
	public void run() {
		while (true) {
			try {
				int i = in.read();

				if (i == -1) {
					q.add_sync(new EOF());
					return;
				} else {
					okSingleton.b = (byte) i;
					q.add_sync(okSingleton);
				}
			} catch (IOException err) {
				q.add_sync(err);
			}
		}
	}

	public static class Ok {
		byte b;
	}

	public static class EOF {

	}

}
