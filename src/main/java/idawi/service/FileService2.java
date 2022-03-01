package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.TypedInnerOperation;
import idawi.MessageQueue;
import idawi.Service;
import idawi.Streams;
import idawi.To;
import idawi.service.DemoService.waiting;
import toools.io.Utilities;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileService2 extends Service {
	final Directory dir = new Directory(directory(), "shared_files");

	public FileService2(Component t) {
		super(t);
		registerOperation(new delete());
		registerOperation(new downloadFile());
		registerOperation(new downloadFileAsOneSingleMessage());
		registerOperation(new listFiles());
		registerOperation(new exists());
		registerOperation(new pathToLocalFiles());
		registerOperation(new size());
		registerOperation(new upload());
	}

	public class pathToLocalFiles extends TypedInnerOperation {
		public String pathToLocalFiles() {
			return dir.getPath();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class listFiles extends TypedInnerOperation {
		public Set<String> listFiles() throws IOException {
			dir.ensureExists();
			List<AbstractFile> files = dir.retrieveTree();
			files.remove(dir);
			return files.stream().map(f -> f.getPath()).collect(Collectors.toSet());
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class downloadFileAsOneSingleMessage extends TypedInnerOperation {
		public byte[] downloadFileAsOneSingleMessage(String path) throws IOException {
			return new RegularFile(dir, path).getContent();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class uploadFileAsOneSingleMessage extends TypedInnerOperation {
		public void uploadFileAsOneSingleMessage(String path, byte[] bytes) throws IOException {
			new RegularFile(dir, path).setContent(bytes);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public void uploadFileAsOneSingleMessage(RegularFile localFile, ComponentDescriptor target, String pathOnTarget)
			throws IOException {
		exec(new To(Set.of(target)).o(FileService2.uploadFileAsOneSingleMessage.class), true,
				parms(pathOnTarget, localFile.getContent()));
	}

	public static waiting fileInfo;

	public static class FileInfo implements Serializable {
		public String name;
		public long len;
		public long age;
	}

	private FileInfo fileInfo(String name) throws IOException {
		var f = new RegularFile(dir, name);
		var info = new FileInfo();
		info.name = name;
		info.len = f.getSize();
		info.age = f.getAgeMs();
		return info;
	}

	public static class DownloadFileParms implements Serializable {
		String name;
		long seek;
		long len;
	}

	public class downloadFile extends TypedInnerOperation {
		public void downloadFile(MessageQueue q) throws IOException {
			var msg = q.get_blocking();
			DownloadFileParms parms = (DownloadFileParms) msg.content;
			dir.ensureExists();
			var f = new RegularFile(dir, parms.name);
			long fileLength = f.getSize();
			var inputStream = f.createReadingStream();
			inputStream.skip(parms.seek);
			Streams.split(inputStream, 1000, c -> send(inputStream, msg.replyTo));
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class upload extends TypedInnerOperation {
		public void upload(String name, boolean append, InputStream in) throws IOException {
			dir.ensureExists();
			var fos = new RegularFile(dir, name).createWritingStream(append);
			Utilities.copy(in, fos);
			fos.close();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class exists extends TypedInnerOperation {
		public boolean exists(String name) {
			dir.ensureExists();
			return new RegularFile(dir, name).exists();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class delete extends TypedInnerOperation {
		public void delete(String name) {
			dir.ensureExists();
			new RegularFile(dir, name).delete();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class size extends TypedInnerOperation {
		public long size(String name) {
			dir.ensureExists();
			return new RegularFile(name).getSize();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
