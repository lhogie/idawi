package idawi.knowledge_base;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

import idawi.Service;

public class ServiceDescriptor implements Serializable {
	public Class<? extends Service> clazz;
	public String description;
	public Set<OperationDescriptor> operations = new TreeSet<>();
	public long nbMessagesReceived = 0;

	@Override
	public String toString() {
		String s = "service " + clazz;

		for (var o : operations) {
			s += "- " + o + "\n";
		}

		s += nbMessagesReceived + " messages received";
		return s;
	}

}
