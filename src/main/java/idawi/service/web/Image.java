package idawi.service.web;

import java.io.IOException;
import java.io.Serializable;

import toools.SizeOf;

public class Image implements Serializable, SizeOf {

	public String base64;

	public static Image random() {
		try {
			var image = new Image();
			image.base64 = Media.download("https://picsum.photos/200/300");
			return image;
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public long sizeOf() {
		// TODO Auto-generated method stub
		return base64.length();
	}
}
