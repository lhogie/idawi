package idawi.bachir;

import java.io.Serial;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;
import idawi.Idawi;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.routing.ComponentMatcher;
import idawi.transport.SIKDriver;
import idawi.transport.SharedMemoryTransport;

public class App {
	public static void main(String[] args) throws InterruptedException {
		Idawi.agenda.start();
		var a = new Component();
		var t = new SIKDriver(a);

		new S(a);

		// new SIKDriver(a);
		// var a_smt = new SharedMemoryTransport(a);

		// var b = new Component();
		// var b_s = new S(b);
		// // new SIKDriver(b);
		// new SharedMemoryTransport(b);

		// a_smt.bcastTargets.add(b);
		while (true) {
			System.out.println("nice");
			t.exec(ComponentMatcher.all, S.class, S.E.class, msg -> {
				msg.content = "blabla";
				System.out.println("sending ");
			});
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
