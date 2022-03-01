package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.TypedInnerOperation;
import idawi.MessageQueue;
import idawi.RemotelyRunningOperation;
import idawi.Service;
import idawi.To;
import toools.extern.ExternalProgram;
import toools.io.file.RegularFile;

public class ExternalCommandsService extends Service {

	private final static Map<String, RegularFile> commandName2executableFile = ExternalProgram.listAvailableCommands();

	public ExternalCommandsService(Component t) {
		super(t);
		registerOperation(new commands());
		registerOperation(new exec());
		registerOperation(new has());
	}

	public class has extends TypedInnerOperation {
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

	public class commands extends TypedInnerOperation {
		public Set<String> f() {
			return commandName2executableFile.keySet();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class exec extends InnerOperation {
		public void exec(MessageQueue in) throws IOException {
			var parmMsg = in.get_blocking();
			List<String> cmdLine = (List<String>) parmMsg.content;
			Process p = Runtime.getRuntime().exec(cmdLine.toArray(new String[0]));
			var stdout = p.getInputStream();
			var stdin = p.getOutputStream();

			newThread(() -> {
				while (true) {
					try {
						byte[] b = stdout.readNBytes(1000);

						if (b.length == 0) {
							break;
						} else {
							reply(parmMsg, b);
						}
					} catch (IOException e) {
						reply(parmMsg, e);
						break;
					}
				}
			});

			while (true) {
				var msg = in.get_blocking();

				if (msg.isEOT()) {
					break;
				}

				if (msg.content instanceof byte[]) {
					stdin.write((byte[]) msg.content);
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

	public static void exec(Service service, ComponentDescriptor b, InputStream in, OutputStream out, String... cmdLine)
			throws IOException {
		var to = new To(Set.of(b)).o(ExternalCommandsService.exec.class);
		RemotelyRunningOperation s = service.exec(to, true, cmdLine);
		boolean eofIN = false;

		while (true) {
			while (s.returnQ.size() > 0) {
				var returnMsg = s.returnQ.get_non_blocking();

				if (returnMsg.isEOT()) {
					return;
				}

				out.write((byte[]) returnMsg.content);
			}

			if (!eofIN) {
				var wav = in.readNBytes(1000);

				if (wav.length == 0) {
					eofIN = true;
				} else {
					s.send(wav);
				}
			}
		}
	}
}
