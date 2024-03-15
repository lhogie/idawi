package idawi.service.local_view;

import java.util.function.Predicate;

import idawi.Component;

public class TrustInfo extends Info {
	Component component;
	double trustValue;

	@Override
	public void exposeComponent(Predicate<Component> p) {
		p.test(component);
	}
}
