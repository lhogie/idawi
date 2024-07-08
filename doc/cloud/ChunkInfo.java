package idawi.service.cloud;

import java.util.ArrayList;
import java.util.List;

class ChunkInfo {
	List<String> hostComponents = new ArrayList<>();
	Hash file;
	long index;
}