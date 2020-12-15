package idawi;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class ByteSource {
	private final InputStream in;
	public final int len;
	private final Object id;

	public ByteSource(InputStream in, int len, Object id) {
		this.in = in;
		this.len = len;
		this.id = id;
	}

	public void forEachChunk(Consumer<Chunk> c) throws IOException {
		int nbChunks = ChunkReceiver.nbChunks(len);

		for (int i = 0; i < nbChunks; ++i) {
			Chunk chunk = new Chunk();
			chunk.len = read(chunk.data);
			chunk.index = i;
			chunk.id = id;
			chunk.nbChunks = nbChunks;
			c.accept(chunk);
		}
	}
	
	private int read(byte[] a) throws IOException {
		int nbRead = 0;

		while (true) {
			int n = in.read(a, nbRead, a.length - nbRead);

			if (n == -1) {
				return nbRead;
			}

			nbRead += n;
		}
	}
}
