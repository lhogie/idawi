package idawi.service.web;

import java.io.Serializable;

import toools.SizeOf;

public class RawData implements Serializable, SizeOf {
	public String base64;
	public String mimeType;

	@Override
	public long sizeOf() {
		return base64.length();
	}
}
