package idawi.demo;

import java.io.IOException;

import idawi.Component;
import idawi.routing.BlindBroadcasting;
import idawi.transport.SharedMemoryTransport;
import toools.io.ser.JavaSerializer;

public class Test {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
	var c = new Component();
	
	var c2 = new JavaSerializer().clone(c.id());
	
	System.out.println(c.id().equals(c2));
	
	}
}
