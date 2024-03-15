package idawi.service.local_view;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import idawi.Component;

public class MiscKnowledgeBase extends KnowledgeBase {

	private Set<TrustInfo> trustInfos = new HashSet<>();
	public Set<Info> misc = new HashSet<>();

	public MiscKnowledgeBase(Component component) {
		super(component);
	}


	public void removeOutdated(double now) {
		misc.removeIf(i -> i.reliability(now) < 0.1);
		trustInfos.removeIf(i -> i.reliability(now) < 0.1);
	}

	@Override
	public String getFriendlyName() {
		return "knowledge base";
	}

	@Override
	public Stream<Info> infos() {
		return Stream.concat(trustInfos.stream(), misc.stream());
	}

}
