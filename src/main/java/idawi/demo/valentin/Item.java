package idawi.demo.valentin;

import toools.SizeOf;

public class Item implements SizeOf {
	String key;
	SizeOf content;

	@Override
	public long sizeOf() {
		return key.length() + content.sizeOf();
	}
}
