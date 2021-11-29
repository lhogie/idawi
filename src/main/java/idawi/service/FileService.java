package idawi.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.InnerClassTypedOperation;
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

	public class pathToLocalFiles extends InnerClassTypedOperation {
		private String pathToLocalFiles() {
			return dir.getPath();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class find extends InnerClassTypedOperation {
		public Set<String> f() throws IOException {
			Cout.debug(dir);
			dir.ensureExists();
			List<AbstractFile> files = dir.retrieveTree();
			Cout.debug(files);
			files.remove(dir);
			var r = files.stream().map(f -> f.getPath()).collect(Collectors.toSet());
			return r;
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class download extends InnerClassTypedOperation {
		public byte[] download(String path) throws IOException {
			return new RegularFile(dir, path).getContent();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class upload extends InnerClassTypedOperation {
		private void f(String path, byte[] bytes) throws IOException {
			new RegularFile(dir, path).setContent(bytes);
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class exists extends InnerClassTypedOperation {
		public boolean exists(String name) {
			dir.ensureExists();
			return new RegularFile(dir, name).exists();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class delete extends InnerClassTypedOperation {
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

	public class size extends InnerClassTypedOperation {
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
