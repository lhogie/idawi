package idawi;

import java.util.Arrays;

public class Utils {
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
	
	public static double time() {
		return System.nanoTime() / 1000000000;
	}
}
