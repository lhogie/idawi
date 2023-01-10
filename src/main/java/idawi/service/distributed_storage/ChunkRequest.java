package idawi.service.distributed_storage;

import it.unimi.dsi.fastutil.ints.IntSet;

public class ChunkRequest {
	ContentAddress addr;
	IntSet requestedChunks;
}