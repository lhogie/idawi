package idawi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import toools.io.OnDiskMap;

public class KeyValueService extends Service {
	public final List<Throwable> errors = new ArrayList<>();
	Map<Object, Object> map = new OnDiskMap<>(directory());

	public KeyValueService(Component peer) {
		super(peer);
	}

	public class get extends TypedInnerClassEndpoint {
		public List<Object> get(List<Object> keys) throws Throwable {
			return keys.stream().map(k -> map.get(k)).toList();
		}

		@Override
		public String getDescription() {
			return "gets one or more values";
		}
	}

	public class keys extends TypedInnerClassEndpoint {
		public Set<Object> get() throws Throwable {
			return map.keySet();
		}

		@Override
		public String getDescription() {
			return "gets a value";
		}
	}

	public class nbKeys extends TypedInnerClassEndpoint {
		public int get() throws Throwable {
			return map.size();
		}

		@Override
		public String getDescription() {
			return "gets the number of keys";
		}
	}

	public class clear extends TypedInnerClassEndpoint {
		public void f() throws Throwable {
			map.clear();
		}

		@Override
		public String getDescription() {
			return "clear the map";
		}
	}

	public class remove extends TypedInnerClassEndpoint {
		public void f(List<Object> keys) throws Throwable {
			map.keySet().removeAll(keys);
		}

		@Override
		public String getDescription() {
			return "remove the given keys";
		}
	}

	public class contains extends TypedInnerClassEndpoint {
		public List<Boolean> f(List<Object> keys) throws Throwable {
			return keys.stream().map(k -> map.containsKey(k)).toList();
		}

		@Override
		public String getDescription() {
			return "checks if the given keys are there";
		}
	}
}
