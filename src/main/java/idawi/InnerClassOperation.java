package idawi;

public abstract class InnerClassOperation extends Operation {

	@Override
	protected Class<? extends Service> getDeclaringService() {
		return (Class<? extends Service>) getClass().getEnclosingClass();
	}

	@Override
	public String getName() {
		return getClass().getName().substring(getClass().getEnclosingClass().getName().length() + 1);
	}
}
