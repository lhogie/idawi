package idawi.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.IdawiOperation;
import idawi.Service;
import toools.io.Cout;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileService extends Service {
	final private Directory dir = new Directory(directory(), "shared_files");

	public FileService(Component t) {
		super(t);
	}

	public static OperationID pathToLocalFiles;

	@IdawiOperation
	private String pathToLocalFiles() {
		return dir.getPath();
	}

	public static OperationID find;

	@IdawiOperation
	private Set<String> find() throws IOException {
		Cout.debug(dir);
		dir.ensureExists();
		List<AbstractFile> files = dir.retrieveTree();
		Cout.debug(files);
		files.remove(dir);
		var r = files.stream().map(f -> f.getPath()).collect(Collectors.toSet());
		return r;
	}

	public static OperationID download;

	@IdawiOperation
	private byte[] download(String path) throws IOException {
		return new RegularFile(dir, path).getContent();
	}

	public static OperationID upload;

	@IdawiOperation
	private void upload(String path, byte[] bytes) throws IOException {
		new RegularFile(dir, path).setContent(bytes);
	}
	
	public static OperationID exists;

	@IdawiOperation
	private boolean exists(String name) {
		dir.ensureExists();
		return new RegularFile(dir, name).exists();
	}

	public static OperationID delete;

	@IdawiOperation
	private void delete(String name) {
		dir.ensureExists();
		new RegularFile(dir, name).delete();
	}

	public static OperationID size;

	@IdawiOperation
	private long size(String name) {
		dir.ensureExists();
		return new RegularFile(name).getSize();
	}
}
