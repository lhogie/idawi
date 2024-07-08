package idawi.service.local_view;

import java.util.ArrayList;
import java.util.List;

public class TypeOperationDescriptor extends EndpointDescriptor {
	public List<String> parameterTypes = new ArrayList<>();

	@Override
	public String toString() {
		return super.toString() + "(" + parameterTypes + ")";
	}

}
