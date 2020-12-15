package idawi;

public class RAMChunkReceiver extends ChunkReceiver {
	final byte[] buf;

	public RAMChunkReceiver(int len) {
		super(len);
		buf = new byte[len];
	}

	@Override
	protected void store(Message msg) {
		var chunk = (Chunk) msg.content;
		System.arraycopy(chunk.data, 0, buf, chunk.index * Chunk.LENGTH, chunk.len);
	}
}