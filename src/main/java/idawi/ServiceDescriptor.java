package idawi;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ServiceDescriptor implements Serializable {
	public String name;
	public Set<OperationDescriptor> operations = new HashSet<>();
	public long nbMessagesReceived = 0;
}
