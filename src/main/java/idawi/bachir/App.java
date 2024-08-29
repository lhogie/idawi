package idawi.bachir;

import idawi.Component;
import idawi.Idawi;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.transport.serial.SerialDriver;

public class App {
	public static void main(String[] args) throws InterruptedException {
		Idawi.agenda.start();
		var a = new Component();
		var t = new SerialDriver(a);
		new S(a);

		// new SIKDriver(a);
		// var a_smt = new SharedMemoryTransport(a);

		// var b = new Component();
		// var b_s = new S(b);
		// // new SIKDriver(b);
		// new SharedMemoryTransport(b);

		// a_smt.bcastTargets.add(b);
		while (true) {
			t.exec(ComponentMatcher.all, S.class, S.E.class, msg -> {
				msg.content = "blabla";
			});
			Thread.sleep(1000);
		}

	}

	public static class S extends Service {
		public S(Component component) {
			super(component);
			System.out.println("Instance S Created");

		}

		public class E extends InnerClassEndpoint<Object, Object> {

			@Override
			public void impl(MessageQueue in) throws Throwable {
				var msg = in.poll_sync();
				System.out.println("exec on b: " + msg);
			}
		}
	}
}
