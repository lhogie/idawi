package idawi;

public abstract class InInnerClassOperation extends Operation {
	private final String name;
	// protected Service service;

	public InInnerClassOperation() {
		String s = getClass().getName();
		int pos = s.lastIndexOf("$");
		this.name = s.substring(pos + 1);
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	protected Class<? extends Service> getDeclaringService() {
		return (Class<? extends Service>) getClass().getDeclaringClass();
	}
}
