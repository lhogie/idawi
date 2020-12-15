package idawi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public abstract class ChunkReceiver {

	final BitSet ok;
	final List<Route> routes = new ArrayList<>();

	ChunkReceiver( int len) {
		this.ok = new BitSet(nbChunks(len));
	}

	public static int nbChunks(int len) {
		int n = len / Chunk.LENGTH;

		if (len / Chunk.LENGTH == 0) {
			return n;
		} else {
			return n + 1;
		}
	}

	void addChunk(Message msg) throws IOException {
		store(msg);
		ok.set(((Chunk) msg.content).index);
	}

	protected abstract void store(Message msg) throws IOException;

	public boolean hasCompleteData() {
		return ok.cardinality() == ok.size();
	}

	public double progress() {
		return ok.cardinality() / (double) ok.size();
	}

}