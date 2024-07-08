package idawi;

public class Utils {
	public static Class<? extends InnerClassEndpoint> innerClass(Class<? extends Service> clazz, String className) {
		for (var ic : clazz.getDeclaredClasses()) {
			if (ic.getName().equals(className)) {
				return (Class<? extends InnerClassEndpoint>) ic;
			}
		}

		return null;
	}
}
