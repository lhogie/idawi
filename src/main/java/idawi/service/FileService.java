package idawi.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.IdawiExposed;
import idawi.Service;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileService extends Service {
	final Directory dir = new Directory(directory(), "shared_files");

	public FileService(Component t) {
		super(t);
	}

	@IdawiExposed
	private String pathToLocalFiles() {
		return dir.getPath();
	}

	@IdawiExposed
	private Set<String> find() throws IOException {
		dir.ensureExists();
		List<AbstractFile> files = dir.retrieveTree();
		files.remove(dir);
		return files.stream().map(f -> f.getPath()).collect(Collectors.toSet());
	}

	@IdawiExposed
	private Set<String> lsFiles(String directory) throws IOException {
		dir.ensureExists();
		var d = new Directory(dir, directory);
		return d.listRegularFiles().stream().map(f -> f.getName()).collect(Collectors.toSet());
	}

	@IdawiExposed
	private Set<String> lsDirectories(String directory) throws IOException {
		dir.ensureExists();
		var d = new Directory(directory);
		return d.listDirectories().stream().map(f -> f.getName()).collect(Collectors.toSet());
	}

	public final static String download = "download";

	@IdawiExposed
	private byte[] download(String path) throws IOException {
		return new RegularFile(dir, path).getContent();
	}

	@IdawiExposed
	private void upload(String path, byte[] bytes) throws IOException {
		new RegularFile(dir, path).setContent(bytes);
	}

	@IdawiExposed
	private boolean exists(String name) {
		dir.ensureExists();
		return new RegularFile(dir, name).exists();
	}

	@IdawiExposed
	private void delete(String name) {
		dir.ensureExists();
		new RegularFile(dir, name).delete();
	}

	@IdawiExposed
	private long size(String name) {
		dir.ensureExists();
		return new RegularFile(name).getSize();
	}
}
