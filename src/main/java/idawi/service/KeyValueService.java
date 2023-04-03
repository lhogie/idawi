package idawi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassOperation;
import toools.io.OnDiskMap;

public class KeyValueService extends Service {
	public final List<Throwable> errors = new ArrayList<>();
	Map<Object, Object> map = new OnDiskMap<>(directory());

	public KeyValueService(Component peer) {
		super(peer);
		registerOperation(new get());
		registerOperation(new contains());
		registerOperation(new keys());
		registerOperation(new nbKeys());
		registerOperation(new remove());
		registerOperation(new clear());
	}

	public class get extends TypedInnerClassOperation {
		public List<Object> get(List<Object> keys) throws Throwable {
			return keys.stream().map(k -> map.get(k)).toList();
		}

		@Override
		public String getDescription() {
			return "gets a value";
		}
	}

	public class keys extends TypedInnerClassOperation {
		public Set<Object> get() throws Throwable {
			return map.keySet();
		}

		@Override
		public String getDescription() {
			return "gets a value";
		}
	}

	public class nbKeys extends TypedInnerClassOperation {
		public int get() throws Throwable {
			return map.size();
		}

		@Override
		public String getDescription() {
			return "gets the number of keys";
		}
	}

	public class clear extends TypedInnerClassOperation {
		public void f() throws Throwable {
			map.clear();
		}

		@Override
		public String getDescription() {
			return "clear the map";
		}
	}

	public class remove extends TypedInnerClassOperation {
		public void f(List<Object> keys) throws Throwable {
			map.keySet().removeAll(keys);
		}

		@Override
		public String getDescription() {
			return "remove the given keys";
		}
	}

	public class contains extends TypedInnerClassOperation {
		public List<Boolean> f(List<Object> keys) throws Throwable {
			return keys.stream().map(k -> map.containsKey(k)).toList();
		}

		@Override
		public String getDescription() {
			return "checks if the given keys are there";
		}
	}
}
