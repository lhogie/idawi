package idawi;

import java.util.Random;

import jexperiment.Plots;
import toools.io.file.Directory;

public class Idawi {
	public static Random prng = new Random();
	public static Plots plots;
	public static final Agenda agenda = new Agenda();

	public static Directory directory;
	public static boolean enableEncryption = false;

	public static Directory setDirectory(String name) {
		directory = new Directory(name);
		directory.ensureEmpty();
		plots = new Plots(new Directory(directory, "plots"));
		return directory;
	}
}
