package idawi.demo.website;

import java.io.IOException;

import idawi.Component;
import idawi.Service;

public class CreateNewService {
	public static void main(String[] args) throws IOException {
		var a = new Component();
		var s= new Service(a);
	}
}
