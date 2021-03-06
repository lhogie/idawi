package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.AsMethodOperation.OperationID;
import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.IdawiOperation;
import idawi.MessageQueue;
import idawi.Service;
import idawi.ServiceAddress;
import idawi.Streams;
import toools.io.Utilities;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileService2 extends Service {
	final Directory dir = new Directory(directory(), "shared_files");

	public FileService2(Component t) {
		super(t);
	}

	public static OperationID pathToLocalFiles;

	@IdawiOperation
	private String pathToLocalFiles() {
		return dir.getPath();
	}

	public static OperationID listFiles;

	@IdawiOperation
	private Set<String> listFiles() throws IOException {
		dir.ensureExists();
		List<AbstractFile> files = dir.retrieveTree();
		files.remove(dir);
		return files.stream().map(f -> f.getPath()).collect(Collectors.toSet());
	}

	public static OperationID downloadFileAsOneSingleMessage;

	@IdawiOperation
	private byte[] downloadFileAsOneSingleMessage(String path) throws IOException {
		return new RegularFile(dir, path).getContent();
	}

	public static OperationID uploadFileAsOneSingleMessage;

	@IdawiOperation
	private void uploadFileAsOneSingleMessage(String path, byte[] bytes) throws IOException {
		new RegularFile(dir, path).setContent(bytes);
	}

	public void uploadFileAsOneSingleMessage(RegularFile localFile, ComponentDescriptor target, String pathOnTarget)
			throws IOException {
		trigger(new ServiceAddress(Set.of(target), FileService2.class), FileService2.uploadFileAsOneSingleMessage, true,
				parms(pathOnTarget, localFile.getContent()));
	}

	public static OperationID fileInfo;

	public static class FileInfo implements Serializable {
		public String name;
		public long len;
		public long age;
	}

	@IdawiOperation
	private FileInfo fileInfo(String name) throws IOException {
		var f = new RegularFile(dir, name);
		var info = new FileInfo();
		info.name = name;
		info.len = f.getSize();
		info.age = f.getAgeMs();
		return info;
	}

	public static OperationID downloadFile;

	public static class DownloadFileParms implements Serializable {
		String name;
		long seek;
		long len;
	}

	@IdawiOperation
	private void downloadFile(MessageQueue q) throws IOException {
		var msg = q.get_blocking();
		DownloadFileParms parms = (DownloadFileParms) msg.content;
		dir.ensureExists();
		var f = new RegularFile(dir, parms.name);
		long fileLength = f.getSize();
		var inputStream = f.createReadingStream();
		inputStream.skip(parms.seek);
		Streams.split(inputStream, 1000, c -> send(inputStream, msg.requester));
	}

	public static OperationID upload;

	@IdawiOperation
	private void upload(String name, boolean append, InputStream in) throws IOException {
		dir.ensureExists();
		var fos = new RegularFile(dir, name).createWritingStream(append);
		Utilities.copy(in, fos);
		fos.close();
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
