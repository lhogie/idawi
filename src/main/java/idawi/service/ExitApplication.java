package idawi.service;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.routing.ComponentMatcher;
import toools.thread.Threads;

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
			System.exit(code);
		}

		@Override
		public String getDescription() {
			return "terminates that JVM";
		}
	}

	public void killAll(int exitCode) {
		component.bb().exec(ExitApplication.class, exit.class, null, ComponentMatcher.all, false, exitCode, true);
		component.forEachService(s -> s.dispose());

		// don't quit immediately otherwise the kill message won't have the time to be sent
		Threads.sleep(1);
		System.exit(exitCode);
	}
}
