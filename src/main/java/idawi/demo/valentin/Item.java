package idawi.demo.valentin;

import java.io.Serializable;

import toools.SizeOf;

public class Item implements SizeOf, Serializable {
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

	@Override
	public String toString() {
		return key + "," + content.length + "B";
	}
	

}
