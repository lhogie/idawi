package idawi.deploy;

import java.io.IOException;

import org.checkerframework.checker.units.qual.m;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.service.local_view.LocalViewService;
import idawi.transport.PipeFromToParentProcess;
import toools.io.ser.JavaSerializer;
import toools.thread.Threads;

public class RemoteMain {
	// this method should be called only by this class
	public static void main(String[] args)
			throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		if (args.length != 1 || !args[0].equals("run_by_a_node")) {
			System.err.println(
					"this main class is not intended to be run  by a user: it has to called by the deployment service");
			System.exit(1);
		}

		try {
			System.out.println("JVM " + System.getProperty("java.vendor") + " " + System.getProperty("java.version")
					+ " is running");

			Idawi.agenda.start();

			System.out.println("instantiating component");
			var child = new Component();

			// create the pipe to the parent
			child.service(PipeFromToParentProcess.class, true);
			var m =new Message(null, null, child);
			PipeFromToParentProcess.sendBytes(child.secureSerializer.toBytes(m));

			child.service(LocalViewService.class, true);

			// prevents the JVM to quit
			while (true) {
				Threads.sleep(1);
//				PipeFromToParentProcess.sysout(child.descriptor());
			}
		} catch (Throwable err) {
			err.printStackTrace(System.err);
			System.err.println("Stopping JVM");
			PipeFromToParentProcess.sendBytes(new JavaSerializer<>().toBytes(err));
			System.out.flush();
			System.err.flush();
			Threads.sleep(1);
			System.exit(1);
		}
	}
}
