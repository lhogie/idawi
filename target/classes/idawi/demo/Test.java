package idawi.demo;

import java.io.IOException;

import idawi.Component;
import idawi.routing.BlindBroadcasting;
import idawi.transport.SharedMemoryTransport;
import toools.io.ser.JavaSerializer;

public class Test {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		
		System.out.println(BlindBroadcasting.ping.class.getName());
		var c = Class.forName(BlindBroadcasting.ping.class.getName());
		System.out.println(c);
	}
}
