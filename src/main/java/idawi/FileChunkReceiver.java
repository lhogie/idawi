package idawi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

import toools.io.file.RegularFile;

public class FileChunkReceiver extends ChunkReceiver {
	final FileChannel f;

	public FileChunkReceiver(int len, RegularFile f) throws IOException {
		super(len);
		this.f = FileChannel.open(Paths.get(f.getPath()));
	}

	@Override
	protected void store(Message msg) throws IOException {
		Chunk chunk = (Chunk) msg.content;
		f.position(chunk.index * Chunk.LENGTH);
		f.write(ByteBuffer.wrap(chunk.data, 0, chunk.len));
	}
}