package idawi;

public abstract class InnerOperation extends Operation {

	public static String name(Class<? extends InnerOperation> c) {
		return c.getName().substring(c.getEnclosingClass().getName().length() + 1);
	}

	public String name() {
		return name(getClass());
	}

	@Override
	protected Class<? extends Service> getDeclaringServiceClass() {
		var c = getClass().getEnclosingClass();

		if (c == null)
			throw new IllegalStateException("operation " + getClass() + " should be declared as a static class");

		return (Class<? extends Service>) c;
	}

	@Override
	public String getName() {
		return name(getClass());
	}
}
