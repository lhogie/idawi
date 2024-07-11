package idawi.service.web;

import java.io.Serializable;

import toools.SizeOf;

public class RawData implements Serializable, SizeOf {
	public byte[] bytes;
	public String mimeType;

	public RawData(byte[] bytes, String mimeType) {
		this.bytes = bytes;
		this.mimeType = mimeType;
	}

	@Override
	public long sizeOf() {
		return bytes.length;
	}

	public static class html extends RawData {
		public html(String html) {
			super(html.getBytes(), "text/html");
		}
	}

	public static class csv extends RawData {
		public csv(String s) {
			super(s.getBytes(), "text/csv");
		}
	}

	public static class javascript extends RawData {
		public javascript(String s) {
			super(s.getBytes(), "application/javascript");
		}
	}

}
