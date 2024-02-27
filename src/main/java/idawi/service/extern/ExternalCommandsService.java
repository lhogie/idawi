package idawi.service.extern;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.RemotelyRunningEndpoint;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.MessageQueue;
import toools.extern.ExternalProgram;
import toools.io.file.RegularFile;

public class ExternalCommandsService extends Service {

	private final static Map<String, RegularFile> commandName2executableFile = ExternalProgram.listAvailableCommands();

	public ExternalCommandsService(Component t) {
		super(t);
	}

	public class has extends TypedInnerClassEndpoint {
		public boolean f(String cmdName) {
			RegularFile f = get(cmdName);
			return f != null && f.exists();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class commands extends TypedInnerClassEndpoint {
		public Set<String> f() {
			return commandName2executableFile.keySet();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class exec extends InnerClassEndpoint {
		public void impl(MessageQueue in) throws IOException {
			var parmMsg = in.poll_sync();
			List<String> cmdLine = (List<String>) parmMsg.content;
			Process p = Runtime.getRuntime().exec(cmdLine.toArray(new String[0]));
			var stdout = p.getInputStream();
			var stdin = p.getOutputStream();

			new Thread(() -> {
				while (true) {
					try {
						byte[] b = stdout.readNBytes(1000);
						reply(parmMsg, b, b.length == 0);
					} catch (IOException e) {
						reply(parmMsg, e, true);
						break;
					}
				}
			}).start();

			while (true) {
				var msg = in.poll_sync();

				if (msg.isEOT()) {
					break;
				}

				// if the command is explicitly fed with binary data
				if (msg.content instanceof byte[]) {
					stdin.write((byte[]) msg.content);
				} else {
					stdin.write(msg.content.toString().getBytes());
				}
			}
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private RegularFile get(String cmdName) {
		if (cmdName.contains("/")) {
			return new RegularFile(cmdName);
		} else {
			return commandName2executableFile.get(cmdName);
		}
	}

	public void exec(Component to, InputStream stdin, OutputStream out, String... cmdLine) throws IOException {
		RemotelyRunningEndpoint s = component.defaultRoutingProtocol().exec(ExternalCommandsService.class, exec.class,
				null, null, true, cmdLine, true);
		boolean eofIN = false;

		while (true) {
			while (s.returnQ.size() > 0) {
				var returnMsg = s.returnQ.poll_async();

				if (returnMsg.isEOT()) {
					return;
				}

				out.write((byte[]) returnMsg.content);
			}

			if (!eofIN) {
				var wav = stdin.readNBytes(1000);
				component.defaultRoutingProtocol().send(wav, wav.length == 0, s.getOperationInputQueueDestination());
			}
		}
	}
}
