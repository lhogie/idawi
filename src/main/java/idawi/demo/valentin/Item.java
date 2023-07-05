package idawi.demo.valentin;

import toools.SizeOf;

public class Item implements SizeOf {
	String key;
	byte[] content;

	public Item() {
		this(null, null);
	}

	public Item(String key, byte[] value) {
		this.key = key;
		this.content = value;
	}

	@Override
	public long sizeOf() {
		return key.length() + content.length;
	}
}
