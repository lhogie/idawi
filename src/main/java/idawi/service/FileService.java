package idawi.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.IdawiExposed;
import idawi.Service;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileService extends Service {
	final private Directory dir = new Directory(directory(), "shared_files");

	public FileService(Component t) {
		super(t);
	}

	public static OperationID pathToLocalFiles;

	@IdawiExposed
	private String pathToLocalFiles() {
		return dir.getPath();
	}

	public static OperationID find;

	@IdawiExposed
	private Set<String> find() throws IOException {
		dir.ensureExists();
		List<AbstractFile> files = dir.retrieveTree();
		files.remove(dir);
		return files.stream().map(f -> f.getPath()).collect(Collectors.toSet());
	}

	public static OperationID download;

	@IdawiExposed
	private byte[] download(String path) throws IOException {
		return new RegularFile(dir, path).getContent();
	}

	public static OperationID upload;

	@IdawiExposed
	private void upload(String path, byte[] bytes) throws IOException {
		new RegularFile(dir, path).setContent(bytes);
	}

	public static OperationID exists;

	@IdawiExposed
	private boolean exists(String name) {
		dir.ensureExists();
		return new RegularFile(dir, name).exists();
	}

	public static OperationID delete;

	@IdawiExposed
	private void delete(String name) {
		dir.ensureExists();
		new RegularFile(dir, name).delete();
	}

	public static OperationID size;

	@IdawiExposed
	private long size(String name) {
		dir.ensureExists();
		return new RegularFile(name).getSize();
	}
}
