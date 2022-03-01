package idawi.service;

import java.io.Serializable;
import java.util.function.Predicate;

import idawi.Component;
import idawi.TypedInnerOperation;
import idawi.Service;

/**
 * Sends an empty message on a queue that is created specifically for the peer
 * to bench.
 */

public class PredicateRunner extends Service {
	public static interface SerializablePredicate extends Predicate<Component>, Serializable {
	}

	public PredicateRunner(Component node) {
		super(node);
	}

	public class Test extends TypedInnerOperation {

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean exec(SerializablePredicate p) throws Throwable {
			return p.test(component);
		}

	}

	@Override
	public String getFriendlyName() {
		return "Runs predicates on that component. Useful for filtering, resource discovery, etc.";
	}
}
