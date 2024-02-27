package idawi.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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

	public static class Chunk implements Serializable {
		String dataSourceID;
		long index;
		long nbChunks;
		byte[] data;
	}

	public static void split(InputStream in, String dataSourceID, long expectedSize, int chunckLength,
			Consumer<Chunk> out) throws IOException {
		long nbChunks = expectedSize / chunckLength;

		// created an additional chunk for end of stream, if needed
		if (expectedSize % chunckLength > 0) {
			++nbChunks;
		}

		for (int nbCreated = 0; nbCreated < nbChunks; ++nbCreated) {
			var chunk = new Chunk();
			chunk.nbChunks = nbChunks;
			chunk.index = nbCreated;
			chunk.data = in.readNBytes(chunckLength);

			if (chunk.data.length == 0) {
				break;
			}

			out.accept(chunk);
		}
	}
}
