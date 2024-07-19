package idawi;

import java.io.Serializable;

import idawi.messaging.MessageQueue;
import j4u.filter.UnixFilter;
import toools.SizeOf;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public abstract class NonJavaEndpoint extends InnerClassEndpoint {

	public static class Output<E> implements Serializable, SizeOf {
		public E msg;
		public boolean error;

		public Output(E l, boolean b) {
			this.msg = l;
			this.error = b;
		}

		@Override
		public long sizeOf() {
			return SizeOf.sizeOf(msg);
		}
	}

	private final Directory directory;

	public NonJavaEndpoint(Directory directory) {
		this.directory = directory;
	}

	@Override
	public void impl(MessageQueue in) throws Throwable {
		var execM = in.poll_sync();
		final Thread t = Thread.currentThread();

		var f = new UnixFilter(directory, executable().getPath()) {

			@Override
			protected void terminated(int returnCode) {
				service.send(new Output<Integer>(returnCode, false), execM.replyTo, m -> m.eot = true);
				t.interrupt();
			}

			@Override
			protected void newLineOnStdout(String l) {
				service.send(new Output<String>(l, false), execM.replyTo);
			}

			@Override
			protected void newLineOnStderr(String l) {
				service.send(new Output<String>(l, true), execM.replyTo);
			}
		};

		while (true) {
			var inM = in.poll_sync();
			f.newLineToStdin(inM.content.toString());

			if (inM.eot) {
				break;
			}
		}
		
		f.endOfTransmission();
	}

	protected abstract RegularFile executable();

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

}
