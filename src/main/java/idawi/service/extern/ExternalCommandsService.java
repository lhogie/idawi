package idawi.service.extern;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.Computation;
import idawi.FunctionEndPoint;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import idawi.messaging.MessageQueue;
import toools.extern.ExternalProgram;
import toools.io.file.RegularFile;

public class ExternalCommandsService extends Service {

	private final static Map<String, RegularFile> commandName2executableFile = ExternalProgram.listAvailableCommands();

	public ExternalCommandsService(Component t) {
		super(t);
	}

	public class has extends FunctionEndPoint<String, Boolean> {
		@Override
		public Boolean f(String cmdName) {
			RegularFile f = get(cmdName);
			return f != null && f.exists();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class commands extends SupplierEndPoint<Set<String>> {
		@Override
		public Set<String> get() {
			return commandName2executableFile.keySet();
		}

		@Override
		public String r() {
			return null;
		}
	}

	public class exec extends InnerClassEndpoint<List<String>, byte[]> {
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
						send(b, parmMsg.replyTo, m -> m.eot = b.length == 0);
					} catch (IOException e) {
						send(e, parmMsg.replyTo, m -> m.eot = true);
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

	public void exec(Component to, InputStream stdin, OutputStream out, List<String> cmdLine) throws IOException {
		Computation s = exec(to, ExternalCommandsService.class, exec.class, msg -> msg.content = cmdLine);

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
				send(wav, s.inputQAddr, m -> m.eot = wav.length == 0);
			}
		}
	}
}
