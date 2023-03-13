package idawi.knowledge_base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import idawi.Component;

public class ComponentRef implements Serializable {

	public transient ComponentDescription description;
	public transient Component component;

	public String ref;

	public ComponentRef() {
		this(new Random().nextLong());
	}

	public ComponentRef(Object v) {
		this.ref = v.toString();
	}

	
	@Override
	public int hashCode() {
		return ref.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ((ComponentRef) obj).ref.equals(ref);
	}

	@Override
	public String toString() {
		return ref;
	}

	public static List<ComponentRef> create(String prefix, int n) {
		var r = new ArrayList<ComponentRef>();

		for (int i = 0; i < n; ++i) {
			r.add(new ComponentRef(prefix + i));
		}

		return r;
	}

	public Long longHash() {
		long h = 1125899906842597L;
		int len = ref.length();
		
		for (int i = 0; i < len; i++) {
			h = 31 * h + ref.charAt(i);
		}
		
		return h;
	}

	public boolean matches(String s) {
		return ref.equals(s);
	}
}
