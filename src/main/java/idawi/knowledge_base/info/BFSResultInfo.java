package idawi.knowledge_base.info;

import java.util.function.Consumer;

import idawi.knowledge_base.BFS.BFSResult;
import idawi.knowledge_base.ComponentRef;

public class BFSResultInfo extends ObjectInfo<BFSResult> {

	public BFSResultInfo(double date, BFSResult r) {
		super(date, r);
	}

	@Override
	public String toString() {
		return "BFS [distances=" + value.distances + "]";
	}

	@Override
	public boolean involves(ComponentRef d) {
		return value.distances.containsKey(d);
	}

	@Override
	public void forEachComponent(Consumer<ComponentRef> c) {
		value.distances.keySet().forEach(ref -> c.accept(ref));
	}

}
