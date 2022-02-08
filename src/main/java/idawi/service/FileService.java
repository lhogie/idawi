package idawi.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.TypedOperation;
import idawi.Service;
import toools.io.Cout;
import toools.io.file.AbstractFile;
import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class FileService extends Service {
	final private Directory dir = new Directory(directory(), "shared_files");
	public find find = new find();
	public delete delete = new delete();
	public download download = new download();
	public exists exists = new exists();
	public size size = new size();
	public upload upload = new upload();
	public pathToLocalFiles pathToLocalFiles = new pathToLocalFiles();

	public FileService(Component t) {
		super(t);
		registerOperation(new delete());
		registerOperation(new download());
		registerOperation(new exists());
		registerOperation(new find());
		registerOperation(new pathToLocalFiles());
		registerOperation(new size());
		registerOperation(new upload());
	}

	public class pathToLocalFiles extends TypedOperation {
		public String pathToLocalFiles() {
			return dir.getPath();
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public class find extends TypedOperation {
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

	public class download extends TypedOperation {
		public byte[] download(String path) throws IOException {
			return new RegularFile(dir, path).getContent();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class upload extends TypedOperation {
		public void f(String path, byte[] bytes) throws IOException {
			new RegularFile(dir, path).setContent(bytes);
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class exists extends TypedOperation {
		public boolean exists(String name) {
			dir.ensureExists();
			return new RegularFile(dir, name).exists();
		}

		@Override
		public String getDescription() {
			return null;
		}
	}

	public class delete extends TypedOperation {
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

	public class size extends TypedOperation {
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
