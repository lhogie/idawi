package idawi.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.Operation;
import idawi.Service;
import toools.extern.ExternalProgram;
import toools.extern.Proces;
import toools.io.file.RegularFile;

public class ExternalCommandsService extends Service {

	public final Map<String, RegularFile> commandName2executableFile = ExternalProgram.listAvailableCommands();

	public static class Req implements Serializable {
		String cmd;
		List<String> parms = new ArrayList<>();
	}

	public ExternalCommandsService(Component t) {
		super(t);
		registerOperation("list path", (m, r) -> r.accept(commandName2executableFile.keySet()));

		registerOperation("has", (m, r) -> {
			String cmdName = (String) m.content;
			RegularFile f = get(cmdName);
			r.accept(f != null && f.exists());
		});

		registerOperation("exec", (m, r) -> {
			var req = (Req) m.content;
			RegularFile f = get(req.cmd);

			if (f == null || !f.exists()) {
				throw new IllegalStateException("unknown command: " + req.cmd);
			} else {
				byte[] stdout = Proces.exec(f.getPath(), req.parms.toArray(new String[0]));
				r.accept(stdout);
			}
		});
	}

	@Operation
	public Set<String> commands() {
		return commandName2executableFile.keySet();
	}

	@Operation
	public String exec(String name, String... parms) {
		RegularFile cmd = commandName2executableFile.get(name);
		return new String(Proces.exec(cmd.getPath(), parms));
	}

	private RegularFile get(String cmdName) {
		if (cmdName.contains("/")) {
			return new RegularFile(cmdName);
		} else {
			return commandName2executableFile.get(cmdName);
		}
	}
}
