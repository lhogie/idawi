package idawi.service.web;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;

public interface Media {

	static String download(String link) throws IOException {
		return Base64.getEncoder().encodeToString(new URL(link).openStream().readAllBytes());
	}
}
