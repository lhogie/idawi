package idawi.service;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.routing.ComponentMatcher;

public class ExitApplication extends Service {

	public ExitApplication(Component peer) {
		super(peer);
	}

	@Override
	public String getFriendlyName() {
		return "exit";
	}

	public class exit extends TypedInnerClassEndpoint {
		public void f(int code) {
			exit(code);
		}

		@Override
		public String getDescription() {
			return "terminates that JVM";
		}
	}

	public void exit(int exitCode) {
		component.bb().exec(ExitApplication.class, exit.class, null, ComponentMatcher.all, false, exitCode);
		component.forEachService(s -> s.dispose());
		System.exit(exitCode);
	}
}
