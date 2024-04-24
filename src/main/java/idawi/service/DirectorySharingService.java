package idawi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.FunctionEndPoint;
import idawi.InnerClassEndpoint;
import idawi.ProcedureEndpoint;
import idawi.Service;
import idawi.SupplierEndPoint;
import idawi.messaging.MessageQueue;
import idawi.messaging.Streams;
import idawi.service.DemoService.waiting;
import idawi.service.DirectorySharingService.downloadFile.DownloadFileParms;
import idawi.service.DirectorySharingService.upload.P;
import toools.io.Utilities;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class DirectorySharingService extends Service {
	final Directory dir = new Directory(directory(), "shared_files");

	public DirectorySharingService(Component t) {
		super(t);
	}

	public class pathToLocalFiles extends SupplierEndPoint<String> {

		@Override
		protected String r() {
			return "gives the path to shared files";
		}

		@Override
		public String get() {
			return dir.getPath();
		}
	}

	public class listFiles extends SupplierEndPoint<Set<String>> {
		@Override
		public Set<String> get() {
			dir.ensureExists();
			List<AbstractFile> files = dir.retrieveTree();
			files.remove(dir);
			return files.stream().map(f -> f.getPath()).collect(Collectors.toSet());
		}

		@Override
		public String r() {
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

	public class downloadFile extends InnerClassEndpoint<DownloadFileParms, byte[]> {
		public static class DownloadFileParms implements Serializable {
			String name;
			long seek = 0;
			long len = Long.MAX_VALUE;
		}

		@Override
		public void impl(MessageQueue q) throws IOException {
			var msg = q.poll_sync();
			var parms = getInputFrom(msg);
			dir.ensureExists();
			var f = new RegularFile(dir, parms.name);
			var inputStream = f.createReadingStream();
			inputStream.skip(parms.seek);
			Streams.split(inputStream, 1000, c -> send(c, msg.replyTo, m -> m.eot = c.length == 0));
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class upload extends ProcedureEndpoint<P> {
		public static class P {
			String name;
			boolean append;
			InputStream in;
		}

		@Override
		public void doIt(P p) throws IOException {
			dir.ensureExists();
			var fos = new RegularFile(dir, p.name).createWritingStream(p.append);
			Utilities.copy(p.in, fos);
			fos.close();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class exists extends FunctionEndPoint<String, Boolean> {

		@Override
		public String getDescription() {
			return "if the file exists";
		}

		@Override
		public Boolean f(String name) throws Throwable {
			return dir.exists() && new RegularFile(dir, name).exists();
		}
	}

	public class delete extends ProcedureEndpoint<String> {
		@Override
		public String getDescription() {
			return "delete the given file";
		}

		@Override
		public void doIt(String name) throws Throwable {
			new RegularFile(dir, name).delete();
		}
	}

	public class size extends FunctionEndPoint<String, Long> {
		@Override
		public Long f(String name) {
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
