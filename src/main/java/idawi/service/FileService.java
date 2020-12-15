package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.ByteSource;
import idawi.Component;
import idawi.Operation;
import idawi.Service;
import toools.io.Utilities;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileService extends Service {

	final Directory dir = new Directory(Component.directory, "shared_files");

	public FileService(Component t) {
		super(t);
	}

	@Operation
	public Set<String> listFiles() throws IOException {
		dir.ensureExists();
		return dir.retrieveTree().stream().map(f -> f.getNameRelativeTo(dir)).collect(Collectors.toSet());
	}

	@Operation
	public ByteSource downloadFile(String name, long seek) throws IOException {
		dir.ensureExists();
		var f = new RegularFile(dir, name);
		long len = f.getSize();
		var is = f.createReadingStream();
		is.skip(seek);
		return new ByteSource(is, (int) (len - seek), name);
	}

	@Operation
	public void upload(String name, boolean append, InputStream in) throws IOException {
		dir.ensureExists();
		var fos = new RegularFile(dir, name).createWritingStream(append);
		Utilities.copy(in, fos);
		fos.close();
	}

	@Operation
	public boolean exists(String name) {
		dir.ensureExists();
		return new RegularFile(dir, name).exists();
	}

	@Operation
	public void delete(String name) {
		dir.ensureExists();
		new RegularFile(dir, name).delete();
	}

	@Operation
	public long size(String name) {
		dir.ensureExists();
		return new RegularFile(name).getSize();
	}

}
