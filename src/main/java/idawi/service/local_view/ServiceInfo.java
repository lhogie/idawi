package idawi.service.local_view;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

import idawi.Service;

public class ServiceInfo implements Serializable {
	public Class<? extends Service> clazz;
	public String description;
	public Set<EndpointDescriptor> endpoints = new TreeSet<>();
	public long nbMessagesReceived = 0;
	public int nbQueues;
	public long sizeOf;

	@Override
	public String toString() {
		String s = "service " + clazz;

		for (var o : endpoints) {
			s += "- " + o + "\n";
		}

		s += nbMessagesReceived + " messages received";
		return s;
	}

}
