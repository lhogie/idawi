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

	public static class html extends RawData {
		public html(String html) {
			mimeType = "text/html";
			bytes = html.getBytes();
		}
	}

	public static class csv extends RawData {
		public csv(String s) {
			mimeType = "text/csv";
			bytes = s.getBytes();
		}
	}

	public static class javascript extends RawData {
		public javascript(String s) {
			mimeType = "application/javascript";
			bytes = s.getBytes();
		}
	}

}
