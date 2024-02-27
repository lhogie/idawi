package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.Service;
import idawi.TypedInnerClassEndpoint;
import idawi.messaging.MessageQueue;
import idawi.messaging.Streams;
import idawi.service.DemoService.waiting;
import toools.io.Utilities;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class DirectorySharingService extends Service {
	final Directory dir = new Directory(directory(), "shared_files");

	public DirectorySharingService(Component t) {
		super(t);
	}
	

	public class pathToLocalFiles extends TypedInnerClassEndpoint {
		public String pathToLocalFiles() {
			return dir.getPath();
		}

		@Override
		public String getDescription() {
			return "gives the path to shared files";
		}
	}

	public class listFiles extends TypedInnerClassEndpoint {
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
		long seek = 0;
		long len = Long.MAX_VALUE;
	}

	public class downloadFile extends TypedInnerClassEndpoint {
		public void downloadFile(MessageQueue q) throws IOException {
			var msg = q.poll_sync();
			var parms = (DownloadFileParms) msg.content;
			dir.ensureExists();
			var f = new RegularFile(dir, parms.name);
			var inputStream = f.createReadingStream();
			inputStream.skip(parms.seek);
			Streams.split(inputStream, 1000, c -> reply(msg, c, c.length == 0));
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class upload extends TypedInnerClassEndpoint {
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

	public class exists extends TypedInnerClassEndpoint {
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

	public class delete extends TypedInnerClassEndpoint {
		public void delete(String name) {
			dir.ensureExists();
			new RegularFile(dir, name).delete();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class size extends TypedInnerClassEndpoint {
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
