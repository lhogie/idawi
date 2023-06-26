package idawi;

public abstract class InnerClassEndpoint extends AbstractEndpoint {

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
}
