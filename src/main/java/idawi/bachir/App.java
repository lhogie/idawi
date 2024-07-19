package idawi.bachir;

import idawi.Component;
import idawi.ProcedureEndpoint;
import idawi.Service;
import idawi.routing.ComponentMatcher;
import idawi.transport.SIKDriver;

public class App {
	public static void main(String[] args) {
		var c = new Component();
		var t = new SIKDriver(c);

		t.exec(ComponentMatcher.all, S.class, S.E.class, msg -> {
			msg.content = "hello";
			System.out.println(msg);
			System.out.println("sending ");
		});
	}

	public class S extends Service {
		public static final long serialVersionUID = 0L;

		public S(Component component) {
			super(component);
		}

		public class E extends ProcedureEndpoint<String> {

			@Override
			public void doIt(String in) throws Throwable {
				System.out.println("received " + in);
			}
		}
	}
}
