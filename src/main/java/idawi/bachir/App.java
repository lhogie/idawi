package idawi.bachir;

import java.io.Serial;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;
import idawi.Idawi;
import idawi.ProcedureEndpoint;
import idawi.Service;
import idawi.bachir.App.S;
import idawi.routing.ComponentMatcher;
import idawi.transport.SIKDriver;

public class App {
	public static void main(String[] args) {
		Idawi.agenda.start();
		var c = new Component();
		var t = new SIKDriver(c);
		new S(c);
		SerialPort comPort = SerialPort.getCommPort("COM8");
		try {
			while (true) {

				t.exec(ComponentMatcher.all, S.class, S.E.class, msg -> {
					msg.content = "hello";
					System.out.println("message : " + msg);
					System.out.println("sending ");

				});
			}
		} catch (Exception e) {
			comPort.closePort();
		}

	}

	public static class S extends Service {
		public static final long serialVersionUID = 0L;

		public S(Component component) {
			super(component);
			System.out.println("Instance S Created");
		}

		public class E extends ProcedureEndpoint<String> {

			@Override
			public void doIt(String in) throws Throwable {

				System.out.println("received " + in);
			}
		}
	}
}
