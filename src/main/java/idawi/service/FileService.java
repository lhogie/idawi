package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.ByteSource;
import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.ExposedOperation;
import idawi.Service;
import idawi.To;
import toools.io.Utilities;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileService extends Service {
	final Directory dir = new Directory(Component.directory, "shared_files");

	public FileService(Component t) {
		super(t);
	}

	@ExposedOperation
	private Set<String> listFiles() throws IOException {
		dir.ensureExists();
		List<AbstractFile> files = dir.retrieveTree();
		files.remove(dir);
		return files.stream().map(f -> f.getPath()).collect(Collectors.toSet());
	}

	public final static String downloadFileAsOneSingleMessage = "downloadFileAsOneSingleMessage";

	@ExposedOperation
	private byte[] downloadFileAsOneSingleMessage(String path) throws IOException {
		return new RegularFile(dir, path).getContent();
	}

	@ExposedOperation
	private void uploadFileAsOneSingleMessage(String path, byte[] bytes) throws IOException {
		new RegularFile(dir, path).setContent(bytes);
	}

	public void uploadFileAsOneSingleMessage(RegularFile localFile, ComponentDescriptor target, String pathOnTarget)
			throws IOException {
		call(new To(target, FileService.class, "uploadFileAsOneSingleMessage"), pathOnTarget, localFile.getContent());
	}

	@ExposedOperation
	private ByteSource downloadFile(String name, long seek) throws IOException {
		dir.ensureExists();
		var f = new RegularFile(dir, name);
		long len = f.getSize();
		var is = f.createReadingStream();
		is.skip(seek);
		return new ByteSource(is, (int) (len - seek), name);
	}

	@ExposedOperation
	private void upload(String name, boolean append, InputStream in) throws IOException {
		dir.ensureExists();
		var fos = new RegularFile(dir, name).createWritingStream(append);
		Utilities.copy(in, fos);
		fos.close();
	}

	@ExposedOperation
	private boolean exists(String name) {
		dir.ensureExists();
		return new RegularFile(dir, name).exists();
	}

	@ExposedOperation
	private void delete(String name) {
		dir.ensureExists();
		new RegularFile(dir, name).delete();
	}

	@ExposedOperation
	private long size(String name) {
		dir.ensureExists();
		return new RegularFile(name).getSize();
	}

}
