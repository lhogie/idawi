package idawi.service;

import java.util.ArrayList;
import java.util.Properties;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerOperation;
import toools.reflect.ClassPath;

public class JVMInfo extends Service {
	public JVMInfo(Component peer) {
		super(peer);
		registerOperation(new properties());
		registerOperation(new classpath());
	}

	@Override
	public String getFriendlyName() {
		return "remote code executing";
	}

	public class properties extends TypedInnerOperation {
		public Properties f() throws Throwable {
			return System.getProperties();
		}

		@Override
		public String getDescription() {
			return "returns the system properties";
		}
	}

	public class classpath extends TypedInnerOperation {
		public ArrayList<String> f() throws Throwable {
			var r = new ArrayList<String>();

			for (var e : ClassPath.retrieveSystemClassPath()) {
				r.add(e.getFile().getName());
			}

			return r;
		}

		@Override
		public String getDescription() {
			return "returns the system classpath";
		}
	}
}
