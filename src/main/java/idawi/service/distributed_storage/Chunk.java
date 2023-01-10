package idawi.service.distributed_storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import toools.io.Utilities;

class Chunk {
	public static final int CHUNK_SIZE = 1024;
	ContentAddress file;
	int index;
	byte[] data;
	int plainSize = -1;

	public void writeToDisk() throws IOException {
		Files.write(toPath(), data);
	}

	public void readFromDisk() throws IOException {
		data = Files.readAllBytes(toPath());
	}

	public String id() {
		return file + "-" + index;
	}

	public Path toPath() {
		return Paths.get(id());
	}

	public boolean isPlain() {
		return plainSize == -1;
	}

	public void compressAndEncrypt() {
		plainSize = data.length;
		data = Utilities.gzip(data);
	}

	public void decompressAndDecrypt() {
		plainSize = -1;
		data = Utilities.gunzip(data);
	}

	public Hash computeHash() {
		if (!isPlain())
			throw new IllegalStateException("can't compute the hash of an encrypted chunk");

		var h = new Hash();
		h.value = Arrays.hashCode(data);
		return h;
	}

}