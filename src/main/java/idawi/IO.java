package idawi;

import java.io.PrintStream;

import toools.util.Date;

public class IO {

	public static void stdout(Object s) {
		stdout(System.out, s);
	}

	public static void stdout(PrintStream o, Object s) {
		o.println(Date.prettyTime(Idawi.agenda.now()) + "\t" + s);
	}

	public static void stderr(Object s) {
		System.err.println(String.format("%.3f", Idawi.agenda.now()) + "\t" + s);
	}

}
