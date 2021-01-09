package idawi.service;

import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.IdawiExposed;
import idawi.Service;
import toools.extern.ExternalProgram;
import toools.extern.Proces;
import toools.io.file.RegularFile;

public class ExternalCommandsService extends Service {

	private final static Map<String, RegularFile> commandName2executableFile = ExternalProgram.listAvailableCommands();

	public ExternalCommandsService(Component t) {
		super(t);
		registerOperation("list path", (m, r) -> r.accept(commandName2executableFile.keySet()));

		registerOperation("has", (m, r) -> {
			String cmdName = (String) m.content;
			RegularFile f = get(cmdName);
			r.accept(f != null && f.exists());
		});
	}



	@IdawiExposed
	private Set<String> commands() {
		return commandName2executableFile.keySet();
	}

	@IdawiExposed
	private String exec(String name, String... parms) {
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
