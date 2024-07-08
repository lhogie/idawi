package idawi.service.cloud;

import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileShape extends Shape {
	public enum TYPE {
		REGULAR, DIRECTORY, SYMLINK
	}

	public FileShape(AbstractFile f) {
		name = f.getPath();
		lastModifiedTime = f.getLastModificationDateMs();

		if (f instanceof Directory) {
			this.type = TYPE.DIRECTORY;
		} else if (f instanceof RegularFile) {
			this.type = TYPE.REGULAR;
		} else {
			throw new IllegalStateException("unknown file type " + f.getClass().getName());
		}

	}

	public TYPE type;
	public String name;
	public long lastModifiedTime;
	public byte chmod;
	public int chown, chgrp;
}
