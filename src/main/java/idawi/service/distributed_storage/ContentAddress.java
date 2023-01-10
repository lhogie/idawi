package idawi.service.distributed_storage;

public class ContentAddress {
	Hash hash;
	long len;

	int nbChunks() {
		return (int) (len / Chunk.CHUNK_SIZE);
	}
}