package idawi.service.distributed_storage;

public class ContentFile {
	public enum TYPE {
		REGULAR, DIRECTORY, SYMLINK
	}

	public String name;
	public long lastModifiedTime;
	public byte chmod;
	public int chown, chgrp;
}
