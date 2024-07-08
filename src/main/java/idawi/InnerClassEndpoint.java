package idawi;

import java.lang.reflect.InvocationTargetException;

public abstract class InnerClassEndpoint<I,O> extends AbstractEndpoint<I, O> {

	public Class<? extends InnerClassEndpoint> id() {
		return getClass();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	protected Class<? extends Service> getDeclaringServiceClass() {
		return (Class<? extends Service>) service.getClass();
	}

	static void registerInnerClassEndpoints(Service s) {
		for (var innerClass : s.getClass().getClasses()) {
			if (InnerClassEndpoint.class.isAssignableFrom(innerClass)) {
				try {
					s.registerEndpoint((InnerClassEndpoint) innerClass.getConstructor(innerClass.getDeclaringClass())
							.newInstance(s));
				} catch (InvocationTargetException | InstantiationException | IllegalAccessException
						| IllegalArgumentException | NoSuchMethodException | SecurityException err) {
					throw new IllegalStateException(err);
				}
			}
		}
	}

}
