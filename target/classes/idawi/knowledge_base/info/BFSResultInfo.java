package idawi.knowledge_base.info;

import java.util.function.Predicate;

import idawi.Component;
import idawi.knowledge_base.BFS.BFSResult;

public class BFSResultInfo extends ObjectInfo<BFSResult> {

	public BFSResultInfo(double date, BFSResult r) {
		super(date, r);
	}

	@Override
	public String toString() {
		return "BFS [distances=" + value.distances + "]";
	}

	@Override
	public void exposeComponent(Predicate<Component> p) {
		for (var c : value.distances.keySet()) {
			if (p.test(c)) {
				return;
			}
		}
	}

}
