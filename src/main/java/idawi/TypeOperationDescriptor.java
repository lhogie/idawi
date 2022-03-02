package idawi;

import java.util.ArrayList;
import java.util.List;

public class TypeOperationDescriptor extends OperationDescriptor {
	public List<String> parameterTypes = new ArrayList<>();

	@Override
	public String toString() {
		return super.toString() + "(" + parameterTypes + ")";
	}

}
