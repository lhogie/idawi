package idawi.service.web;

import java.io.IOException;
import java.io.Serializable;
public class Video implements Serializable {

    public String url;
    public String base64;

    public Video(String url) {
        this.url = url;
    }

    public static Video random() throws IOException {
        var video = new Video("https://www.youtube.com/embed/5qap5aO4i9A");
        video.base64 = Media.toBase64(video.url);
        return video;
    }
}
