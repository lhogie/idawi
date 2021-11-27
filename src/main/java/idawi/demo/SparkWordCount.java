package idawi.demo;

import java.io.IOException;
import java.util.Set;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.AsMethodOperation.OperationID;
import idawi.service.DeployerService;
import idawi.service.DummyService;

public class SparkWordCount {
	
	static class WordCount {
		static OperationID count = null;	
	}
	
	public static void main(String[] args) throws IOException {
		ServiceAddress t = null;
		Service s = null;
		String line = null;
		
		s.exec(t, WordCount.count, 0, 0, line.split(" ")).
	}
}
