package idawi.service.web;

import java.io.Serializable;

import toools.SizeOf;

public class RawData implements Serializable, SizeOf {
	public byte[] bytes;
	public String mimeType;

	@Override
	public long sizeOf() {
		return bytes.length;
	}
}
