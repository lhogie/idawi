package idawi;

public abstract class InnerOperation extends AbstractOperation {
	public static <S extends Service> Class<S> serviceClass(Class<? extends InnerOperation> c) {
		return (Class<S>) c.getEnclosingClass();
	}

	public static String name(Class<? extends InnerOperation> c) {
		int afterDollar = c.getEnclosingClass().getName().length() + 1;
		return c.getName().substring(afterDollar);
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
