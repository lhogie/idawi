package idawi.service.web;

import java.io.IOException;
import java.io.Serializable;

import toools.SizeOf;

public class Image implements Serializable, SizeOf {

	public String url;
	public String base64;

	public Image(String url) {
		this.url = url;
	}

	public static Image random() throws IOException {
		var image = new Image("https://picsum.photos/200/300");
		image.base64 = Media.toBase64(image.url);
		return image;
	}

	@Override
	public long sizeOf() {
		// TODO Auto-generated method stub
		return base64.length();
	}
}
