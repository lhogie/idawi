package idawi.service;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import idawi.routing.TargetComponents;

public class ExitApplication extends Service {

	public ExitApplication(Component peer) {
		super(peer);
		registerOperation(new exit());
	}

	@Override
	public String getFriendlyName() {
		return "exit";
	}

	public class exit extends TypedInnerClassOperation {
		public void f(int code) {
			exit(code);
		}

		@Override
		public String getDescription() {
			return "terminates that JVM";
		}
	}

	public void exit(int exitCode) {
		component.bb().exec(ExitApplication.exit.class, null, TargetComponents.all, false, exitCode);
		component.forEachService(s -> s.dispose());
		System.exit(exitCode);
	}
}
