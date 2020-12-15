package idawi;

import java.util.ArrayList;

public class OperationStringParameterList extends ArrayList<String> {
	public OperationStringParameterList(String... parms) {
		for (var o : parms) {
			add(o);
		}
	}
}