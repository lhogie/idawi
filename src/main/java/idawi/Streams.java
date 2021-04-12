package idawi;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class Streams {
	public static void split(InputStream in, int chunckLength, Consumer<byte[]> out) throws IOException {
		while (true) {
			byte[] b = in.readNBytes(chunckLength);

			if (b.length == 0) {
				break;
			}

			out.accept(b);
		}
	}

}
