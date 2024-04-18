package idawi.service;

import idawi.Component;
import idawi.ProcedureEndpoint;
import idawi.Service;
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

	public class exit extends ProcedureEndpoint<Integer> {


		@Override
		public String getDescription() {
			return "terminates that JVM";
		}

		@Override
		public void doIt(Integer exitCode) throws Throwable {
			System.exit(exitCode);

		}
	}

	public void killAll(int exitCode) {
		exec(ComponentMatcher.all, ExitApplication.class, exit.class, msg -> msg.content = exitCode);
		component.forEachService(s -> s.dispose());

		// don't quit immediately otherwise the kill message won't have the time to be
		// sent
		Threads.sleep(1);
		System.exit(exitCode);
	}
}
