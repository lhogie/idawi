package idawi.service.cloud;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

class Content {
	Hash hash;
	Int2ObjectMap<Chunk> chunks = new Int2ObjectOpenHashMap<>();
	Map<String, Set<? extends Shape>> owners = new HashMap<>();

	public Hash computeHash() {
		if (!isComplete())
			throw new IllegalStateException("can't compute the hash of an incomplete content");

		var chunkHashes = chunks.values().stream().map(c -> c.computeHash());
		return Hash.merge(chunkHashes.collect(Collectors.toList()));
	}

	public boolean isComplete() {
		int nbChunks = (int) (hash.length / Chunk.CHUNK_SIZE);
		return nbChunks == chunks.size();
	}

	public double completionRatio() {
		return chunks.size() / (double) (hash.length / Chunk.CHUNK_SIZE);
	}

	public IntSet missingChunksIndices() {
		var r = new IntOpenHashSet();
		int nbChunks = (int) (hash.length / Chunk.CHUNK_SIZE);

		for (int i = 0; i < nbChunks; ++i) {
			if (!chunks.containsKey(i)) {
				r.add(i);
			}
		}

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
		int nbMissing = missingChunksIndices().size();

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

}