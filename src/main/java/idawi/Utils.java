package idawi;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import idawi.transport.OutLinks;
import idawi.transport.Topologies;
import toools.io.file.RegularFile;
import toools.reflect.Clazz;

public class Utils {
	public static final Random random = new Random();

	public static String prettyTime(double t) {
		return String.format("%.3f", t) + "s";
	}
	
	// Objects.equals() does not support arrays
	public static boolean equals(Object a, Object b) {
		if (a == null) {
			return b == null;
		}

		if (a.getClass().isArray()) {
			return Arrays.deepEquals((Object[]) a, (Object[]) b);
		} else {
			return a.equals(b);
		}
	}

	public static Throwable cause(Throwable t) {
		while (t.getCause() != null) {
			t = t.getCause();
		}

		return t;
	}

	public static double loadRatio() {
		return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()
				/ (double) Runtime.getRuntime().availableProcessors();
	}

	private static String innerClassName(Class c) {
		var ec = c.getEnclosingClass();

		if (ec == null)
			throw new IllegalArgumentException(c + " is not an inner class");

		return c.getName().substring(ec.getName().length() + 1);
	}

	private static Class enclosingClass(Object lambda) {
		int i = lambda.getClass().getName().indexOf("$$Lambda$");

		if (i < 0) {
			throw new IllegalStateException("this is not a lambda");
		}

		return Clazz.findClass(lambda.getClass().getName().substring(0, i));
	}

	public static Class<? extends InnerClassEndpoint> innerClass(Class<? extends Service> clazz, String className) {
		for (var ic : clazz.getDeclaredClasses()) {
			if (ic.getName().equals(className)) {
				return (Class<? extends InnerClassEndpoint>) ic;
			}
		}

		return null;
	}

}
