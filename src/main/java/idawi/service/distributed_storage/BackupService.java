package idawi.service.distributed_storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.MessageQueue;
import idawi.NeighborhoodListener;
import idawi.OperationAddress;
import idawi.Service;
import idawi.To;
import idawi.TypedInnerOperation;
import idawi.net.NetworkingService;
import idawi.net.TransportLayer;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class BackupService extends Service {
	Set<Content> localChunks = new HashSet<>();

	final Set<ChunkInfo> infos = new HashSet<>();

	public BackupService(Component component) {
		super(component);
		operations.add(new ServeChunk());

		component.lookup(NetworkingService.class).transport.listeners.add(new NeighborhoodListener() {

			@Override
			public void newNeighbor(ComponentDescriptor peer, TransportLayer protocol) {
				var to = new To(peer);
				exec(to.o(BackupService.ServeChunk.class), false, requests());
			}

			@Override
			public void neighborLeft(ComponentDescriptor peer, TransportLayer protocol) {
				// TODO Auto-generated method stub

			}
		});
	}

	public ContentRequest requests() {
		var r = new ContentRequest();
		incompleteContents().forEach(ic -> r.put(ic.file, ic.missing()));
		return r;
	}

	public Set<Content> files() {
		return null;
	}

	public Set<Content> incompleteContents() {
		return files().stream().filter(f -> f.isComplete()).collect(Collectors.toSet());
	}

	public Chunk get(ContentAddress file, int i) {
		for (var f : localChunks) {
			if (f.equals(file)) {
				return f.get(i);
			}
		}

		return null;
	}

	public Chunk get(ContentAddress file, int i, Chunk c) {
		for (var f : localChunks) {
			if (f.file.equals(file)) {
				f.set(i, c);
			}
		}

		return null;
	}

	public class ServeChunk extends InnerOperation {

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var tm = in.poll_sync(0);
			var req = (ContentRequest) tm.content;

			for (var e : req.entrySet()) {
				var localContent = lookupLocally(e.getKey());

				if (localContent != null) {
					e.getValue().forEach((int missingChunk) -> {
						var chunk = localContent.chunks.get(missingChunk);

						if (chunk != null) {
							To to = new To(tm.route.source().component);
							OperationAddress o = to.o(BackupService.ReceiveChunk.class);
							BackupService.this.exec(o, false, chunk);
						}
					});
				}
			}

		}

	}

	public Content lookupLocally(ContentAddress addr) {
		for (var c : localChunks) {
			if (c.file.equals(addr)) {
				return c;
			}
		}

		return null;
	}

	public class ListChunksForFile extends TypedInnerOperation {

		public IntSet f(ContentAddress addr) {
			IntSet s = new IntOpenHashSet();

			for (var c : localChunks) {
				if (c.file.equals(addr)) {
					s.addAll(c.indices());
				}
			}

			return s;
		}

		@Override
		public String getDescription() {
			return null;
		}

	}

	public class ListChunksForOwner extends TypedInnerOperation {

		public class R {
			Set<ContentFile> attr;
			IntSet chunkIndices;
		}

		public List<R> f(String owner) {
			var r = new ArrayList<R>();

			for (var chunk : localChunks) {
				Set<ContentFile> attr = chunk.owners.get(owner);

				if (attr != null) {
					var i = new R();
					i.attr = attr;
					i.chunkIndices = chunk.indices();
					r.add(i);
				}
			}

			return r;
		}

		@Override
		public String getDescription() {
			return null;
		}

	}

	public class ReceiveChunk extends TypedInnerOperation {

		public void f(Chunk c) {
			// c.
		}

		@Override
		public String getDescription() {
			return null;
		}

	}

}
