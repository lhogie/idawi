package idawi;

import java.io.Serializable;

class Chunk implements Serializable {
	public static final int LENGTH = 32000;

	Object id;
	int index;
	int nbChunks;
	byte[] data = new byte[LENGTH];
	int len;
	int totalLen;

}