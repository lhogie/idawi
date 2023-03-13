package idawi.service.cloud;

import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntSet;

public interface ContentListener {
	void contentCreated(Content c, Set<Shape> shapes);

	void contentDeleted(Content c);

	void contentUpdated(Content c, IntSet chunkIndices);
}
