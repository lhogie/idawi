package idawi;

import java.lang.management.ManagementFactory;

import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class ProcessContentFile {
	public static final Directory processDirectory = new Directory(Component.directory, "running");
	public static final RegularFile processFile = new RegularFile(processDirectory,
			"" + ManagementFactory.getRuntimeMXBean().getPid());

	static {
		if ( ! processDirectory.exists()) {
			processDirectory.mkdirs();
		}
	}
}
