package idawi.demo;

import java.io.IOException;

import idawi.Component;

public class Test {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		var c = new Component();
		System.out.println(c.publicKey());
	}
}
