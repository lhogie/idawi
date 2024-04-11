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
	public static void main(String[] args)
			throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		try {
			System.out.println("JVM " + System.getProperty("java.vendor") + " " + System.getProperty("java.version")
					+ " is running");

			Idawi.agenda.start();
			System.out.println("instantiating component");
			var child = new Component();
			child.friendlyName = args[0];
			child.need(Pipe_ChildSide.class);
			child.need(PingService.class);
			child.need(LocalViewService.class);
			Threads.sleepForever();
		} catch (Throwable err) {
			err.printStackTrace(System.err);
			Pipe_ChildSide.sendBytes(new JavaSerializer<>().toBytes(err));
			System.out.flush();
			System.err.flush();
			Threads.sleep(1);
			System.exit(1);
		}
	}
}
