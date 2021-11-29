package idawi;

public abstract class InnerClassOperation extends Operation {

	public static String name(Class<? extends InnerClassOperation> c) {
		return c.getName().substring(c.getEnclosingClass().getName().length() + 1);
	}
	
	
	@Override
	protected Class<? extends Service> getDeclaringService() {
		return (Class<? extends Service>) getClass().getEnclosingClass();
	}

	@Override
	public String getName() {
		return name(getClass());
	}
}
