package idawi.service;

import idawi.Component;
import idawi.Service;
import idawi.Streams;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileStreamService extends Service {

	public final Directory baseDir = new Directory("$HOME/.public/");

	public FileStreamService(Component node) {
		super(node);
		registerOperation("get",
				(m, r) -> Streams.stream(new RegularFile(baseDir, (String) m.content).createReadingStream(), r));
		registerOperation("test", (msg, returns) -> returns.accept(new RegularFile(baseDir, (String) msg.content).exists()));
	}
}
