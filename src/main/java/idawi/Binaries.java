package idawi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import toools.io.Hasher;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.reflect.ClassPath;

public class Binaries {
	public static List<RegularFile> binaryFiles;

	static {
		binaryFiles = binaryFiles();
	}

	public static List<RegularFile> binaryFiles() {
		var files = new ArrayList<RegularFile>();

		for (var e : ClassPath.retrieveSystemClassPath()) {
			if (e.getFile() instanceof RegularFile) {
				files.add((RegularFile) e.getFile());
			} else {
				((Directory) e.getFile()).search(f -> {
					if (f instanceof RegularFile) {
						files.add((RegularFile) f);
					}

					return false;
				});
			}
		}

		Collections.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
		return files;
	}

	public static long hashBinaries() throws IOException {
		var h = new Hasher();

		for (var e : binaryFiles) {
			e.hashContents(h);
		}

		// Directory.getHomeDirectory().getChildDirectory("jre").hashContents(h);
		return h.result();
	}

	public static long binarySize() {
		long s = 0;

		for (var f : binaryFiles) {
			s += f.getSize();
		}

		return s;
	}

	public static long[] randomOffsets(long size, int n) throws IOException {
		var offsets = new long[n];
		var r = new Random();

		for (int i = 0; i < n; ++i) {
			offsets[i] = r.nextLong(size);
		}

		return offsets;
	}

	public static byte[] proofOfBinaries(long... offsets) {
		byte[] bytes = new byte[offsets.length];

		for (int i = 0; i < offsets.length; ++i) {
			bytes[i] = proofOfBinaries(offsets[i]);
			break;
		}

		return bytes;
	}

	public static byte proofOfBinaries(long offset) {
		for (var f : binaryFiles) {
			if (f.getSize() > offset) {
				return f.read((int) offset, 1)[0];
			}

			offset -= f.getSize();
		}

		return -1;
	}

}
