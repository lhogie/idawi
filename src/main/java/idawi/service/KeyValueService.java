package idawi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.FunctionEndPoint;
import idawi.InnerClassEndpoint;
import idawi.ProcedureEndpoint;
import idawi.ProcedureNoInputEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import toools.io.OnDiskMap;

public class KeyValueService extends Service {
	public final List<Throwable> errors = new ArrayList<>();
	Map<Object, Object> map = new OnDiskMap<>(directory());

	public KeyValueService(Component peer) {
		super(peer);
	}

	public class get extends FunctionEndPoint<List<Object>, List<Object>> {
		@Override
		public List<Object> f(List<Object> keys) throws Throwable {
			return keys.stream().map(k -> map.get(k)).toList();
		}

		@Override
		public String getDescription() {
			return "gets one or more values";
		}
	}

	public class keys extends SupplierEndPoint<Set<Object>> {
		@Override
		public Set<Object> get() throws Throwable {
			return map.keySet();
		}

		@Override
		public String getDescription() {
			return "a value";
		}
	}

	public class nbKeys extends SupplierEndPoint<Integer> {
		public Integer get() throws Throwable {
			return map.size();
		}

		@Override
		public String getDescription() {
			return "the number of keys";
		}
	}

	public class clear extends ProcedureNoInputEndpoint {
		@Override
		public void doIt() throws Throwable {
			map.clear();
		}

		@Override
		public String getDescription() {
			return "clear the map";
		}
	}

	public class remove extends ProcedureEndpoint<List<Object>> {
		@Override
		public void doIt(List<Object> keys) throws Throwable {
			map.keySet().removeAll(keys);
		}

		@Override
		public String getDescription() {
			return "remove the given keys";
		}
	}

	public class contains extends FunctionEndPoint<List<Object>, List<Boolean>> {
		@Override
		public List<Boolean> f(List<Object> keys) throws Throwable {
			return keys.stream().map(k -> map.containsKey(k)).toList();
		}

		@Override
		public String getDescription() {
			return "checks if the given keys are there";
		}
	}
}
