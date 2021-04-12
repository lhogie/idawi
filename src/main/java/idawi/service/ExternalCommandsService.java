package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.ComponentAddress;
import idawi.ComponentDescriptor;
import idawi.IdawiExposed;
import idawi.InInnerClassOperation;
import idawi.InInnerClassTypedOperation;
import idawi.MessageQueue;
import idawi.RunningOperation;
import idawi.Service;
import idawi.QueueAddress;
import idawi.AsMethodOperation.OperationID;
import toools.extern.ExternalProgram;
import toools.io.file.RegularFile;

public class ExternalCommandsService extends Service {

	private final static Map<String, RegularFile> commandName2executableFile = ExternalProgram.listAvailableCommands();

	public ExternalCommandsService(Component t) {
		super(t);
		registerOperation("list path", in -> reply(in.get_blocking(), commandName2executableFile.keySet()));

		registerOperation("has", in -> {
			var msg = in.get_blocking();
			String cmdName = (String) msg.content;
			RegularFile f = get(cmdName);
			reply(msg, f != null && f.exists());
		});
	}

	public static OperationID commands;

	@IdawiExposed
	public class commands extends InInnerClassTypedOperation {
		private Set<String> f() {
			return commandName2executableFile.keySet();
		}
	}

	public static OperationID exec;
	@IdawiExposed
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
	

	private RegularFile get(String cmdName) {
		if (cmdName.contains("/")) {
			return new RegularFile(cmdName);
		} else {
			return commandName2executableFile.get(cmdName);
		}
	}

	public static void main(String[] args) throws IOException {
		Component a = new Component();
		ComponentDescriptor b = ComponentDescriptor.fromCDL("ssh=musclotte.inria.fr");
		a.lookupService(DeployerService.class).deploy(Set.of(b), true, 10, true,
				feedback -> System.out.println(feedback), peerOk -> System.out.println(peerOk));

		var in = new RegularFile("$HOME/a.wav").createReadingStream();
		var out = new RegularFile("$HOME/a.mp3").createWritingStream();

		ExternalCommandsService.exec(new Service(), b, in, out, "lame", "-", "-");
	}

	public static void exec(Service service, ComponentDescriptor b, InputStream in, OutputStream out, String... cmdLine)
			throws IOException {
		RunningOperation s = service.exec(new ComponentAddress(Set.of(b)), ExternalCommandsService.exec, true, cmdLine);
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
