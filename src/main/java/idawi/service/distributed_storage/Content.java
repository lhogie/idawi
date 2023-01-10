package idawi.service.distributed_storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

class Content {
	ContentAddress file;
	Int2ObjectMap<Chunk> chunks = new Int2ObjectOpenHashMap<>();
	Map<String, Set<ContentFile>> owners = new HashMap<>();

	public IntSet indices() {
		return chunks.keySet();
	}

	public Hash computeHash() {
		if (!isComplete())
			throw new IllegalStateException("can't compute the has of an incomplete content");

		List<Hash> chunkHashes = new ArrayList<>(chunks.size());

		for (var c : chunks.values()) {
			chunkHashes.add(c.computeHash());
		}

		return Hash.merge(chunkHashes);
	}

	public Chunk get(int i) {
		return chunks.get(i);
	}

	public boolean isComplete() {
		int nbChunks = file.nbChunks();

		for (int i = 0; i < nbChunks; ++i) {
			if (!chunks.containsKey(i)) {
				return false;
			}
		}

		return true;
	}

	public double completionRatio() {
		return chunks.size() / (double) file.nbChunks();
	}

	public IntSet missing() {
		var r = new IntOpenHashSet();
		int nbChunks = file.nbChunks();

		for (int i = 0; i < nbChunks; ++i) {
			r.add(i);
		}

		r.removeAll(indices());
		return r;
	}

	public void read(byte[] buf, int offset, int posInFile, int len) {
		for (int i = posInFile; i < posInFile + len; ++i) {
			int chunkIndex = posInFile / Chunk.CHUNK_SIZE;
			var chunk = chunks.get(chunkIndex);
			int indexInChunk = posInFile % Chunk.CHUNK_SIZE;
			buf[i - posInFile] = chunk.data[indexInChunk];
		}
	}

	public byte[] content() {
		int nbMissing = missing().size();

		if (nbMissing > 0)
			throw new IllegalStateException(nbMissing + " chunks missing");

		int nbChunks = chunks.size();
		var bytes = new ByteArrayOutputStream(nbChunks * Chunk.CHUNK_SIZE);

		for (int i = 0; i < nbChunks; ++i) {
			var chunk = chunks.get(i);
			try {
				bytes.write(chunk.data);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		return bytes.toByteArray();
	}

	public void set(int i, Chunk c) {
		chunks.put(i, c);
	}
}