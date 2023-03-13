package idawi.knowledge_base;

import java.util.function.Consumer;

public class TrustInfo extends Info {
	ComponentRef component;
	double trustValue;

	@Override
	public boolean involves(ComponentRef d) {
		return d.equals(component);
	}

	@Override
	public void forEachComponent(Consumer<ComponentRef> c) {
		c.accept(component);
	}

}
