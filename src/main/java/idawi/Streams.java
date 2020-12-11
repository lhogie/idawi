package idawi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;

public class Streams {

	public static void stream(InputStream s, Consumer<Object> returns) throws IOException {
		byte[] buf = new byte[1024];

		while (true) {
			int n = s.read(buf);

			if (n == -1) {
				break;
			} else if (n > 0) {
				returns.accept(Arrays.copyOf(buf, n));
			}
		}
		s.close();
	}

	public static void stream(InputStream is, Service service, To to) throws IOException {
		BufferedInputStream bis;
		byte[] buf = new byte[1024];

		while (true) {
			int n = is.read(buf);

			if (n == -1) {
				break;
			} else if (n > 0) {
				service.send(Arrays.copyOf(buf, n), to, null);
			}
		}
		
		is.close();
	}
}
