package idawi.deploy;

import java.io.IOException;

import idawi.Component;
import idawi.Idawi;
import idawi.service.PingService;
import idawi.service.local_view.LocalViewService;
import idawi.transport.Pipe_ChildSide;
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
			var pipe = child.need(Pipe_ChildSide.class);
			child.friendlyName = "baby";
//			Pipe_ChildSide.sendBytes(pipe.serializer.toBytes(child));
			child.need(PingService.class);
			child.need(LocalViewService.class);

			// prevents the JVM to quit
			while (true) {
				Threads.sleep(1);
//				PipeFromToParentProcess.sysout(child.descriptor());
			}
		} catch (Throwable err) {
			err.printStackTrace(System.err);
			System.err.println("Stopping JVM");
			Pipe_ChildSide.sendBytes(new JavaSerializer<>().toBytes(err));
			System.out.flush();
			System.err.flush();
			Threads.sleep(1);
			System.exit(1);
		}
	}
}
