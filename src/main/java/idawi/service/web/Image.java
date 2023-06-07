package idawi.service.web;

import java.io.*;

public class Image implements Serializable {

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
}
