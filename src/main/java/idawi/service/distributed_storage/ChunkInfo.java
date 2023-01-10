package idawi.service.distributed_storage;

import java.util.ArrayList;
import java.util.List;

class ChunkInfo {
	List<String> hostComponents = new ArrayList<>();
	ContentAddress file;
	long index;
}