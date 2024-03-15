package idawi.routing;

import java.io.Serializable;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import toools.SizeOf;

public class AutoForgettingLongList implements Serializable, SizeOf {
	LongArrayList l = new LongArrayList();
	private Predicate<AutoForgettingLongList> p;

	public AutoForgettingLongList(Predicate<AutoForgettingLongList> p) {
		this.p = p;
	}

	public void add(long i) {
		l.add(i);
	}

	public void ensureOk() {
		while (!p.test(this)) {
			l.removeElements(0, 1);
		}
	}

	public int size() {
		return l.size();
	}

	@Override
	public long sizeOf() {
		return 8 + 8 * size();
	}

	@Override
	public String toString() {
		return l.stream().map(l -> Long.toHexString(l)).toList().toString();
	}

	
	public boolean contains(long iD) {
		return l.contains(iD);
	}

	public LongArrayList asLongArrayList() {
		return l;
	}
}
