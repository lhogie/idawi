package idawi.service.cloud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import idawi.Component;
import idawi.InnerOperation;
import idawi.NeighborhoodListener;
import idawi.OperationAddress;
import idawi.Service;
import idawi.TypedInnerOperation;
import idawi.knowledge_base.CR;
import idawi.knowledge_base.KnowledgeBase;
import idawi.knowledge_base.NetworkTopologyListener;
import idawi.messaging.MessageQueue;
import idawi.routing.irp.IRPTo;
import idawi.transport.NetworkingService;
import idawi.transport.TransportService;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import toools.io.file.Directory;

public class CloudService extends Service {
	final Directory contentDirectory = new Directory("$HOME/idawi/cloud/files");
	Set<Content> localContents = new HashSet<>();
	String name;

	Map<String, MeetingHistory> meetingHistory = new HashMap<>();

	final Set<ChunkInfo> infos = new HashSet<>();

	public CloudService(Component component) {
		super(component);

		if (!contentDirectory.exists()) {
			contentDirectory.mkdirs();
		}

		contentDirectory.retrieveTree().forEach(f -> {
			Content c = new Content();
			c.owners.put(name, Set.of(new FileShape(f)));
			
			
		});

		operations.add(new ServeChunk());

		// update meeting history information
		component.lookup(KnowledgeBase.class).map.listener.add(new NetworkTopologyListener() {
	
			@Override
			public void newComponent(CR p) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void newEdge(CR u, CR v) {
				if (u.equals(component.descriptor()) && u.equals(component.descriptor())){
					var t = meetingHistory.get(peer.name);

					if (t == null) {
						meetingHistory.put(peer.name, t = new MeetingHistory());
					}

					t.add(new Meeting(now()));
				}
				
			}

			@Override
			public void edgeRemoved(CR u, CR v) {
				var t = meetingHistory.get(peer.name);

				if (t != null) {
					t.lastMeeting().endDate = now();
				}			}
		});

		// request missing files
		component.outs(NetworkingService.class).transport.listeners.add(new NeighborhoodListener() {

			@Override
			public void newNeighbor(CR peer, TransportService protocol) {
				var to = new IRPTo(peer);
				exec(to.o(CloudService.ServeChunk.class), false, requests());
			}

			@Override
			public void neighborLeft(CR peer, TransportService protocol) {
			}
		});

		new ContentListener() {

			@Override
			public void contentUpdated(Content c, IntSet chunkIndices) {

			}

			@Override
			public void contentDeleted(Content c) {

			}

			@Override
			public void contentCreated(Content c, Set<Shape> shapes) {
				localContents.add(c); // register it
				c.owners.put(name, shapes); // belongs to me
			}
		};
	}

	public double proximity(String node) {
		if (node.equals(name)) {
			return 1;
		}

		var h = meetingHistory.get(node);

		if (h == null || h.isEmpty()) {
			return 0;
		} else {
			return h.totalMeetingTime() / h.totalTime();
		}
	}

	public ContentRequest requests() {
		var r = new ContentRequest();
		incompleteContents().forEach(ic -> r.put(ic.hash, ic.missingChunksIndices()));
		return r;
	}

	public Set<Content> files() {
		return null;
	}

	public Set<Content> incompleteContents() {
		return files().stream().filter(f -> f.isComplete()).collect(Collectors.toSet());
	}

	public Chunk get(Hash addr, int i) {
		return lookupLocalContent(addr).chunks.get(i);
	}

	public void set(Hash addr, int i, Chunk c) {
		lookupLocalContent(addr).chunks.put(i, c);
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
				var localContent = lookupLocalContent(e.getKey());

				if (localContent != null) {
					e.getValue().forEach((int missingChunk) -> {
						var chunk = localContent.chunks.get(missingChunk);

						if (chunk != null) {
							IRPTo to = new IRPTo(tm.route.initialEmission().component);
							OperationAddress o = to.o(CloudService.ReceiveChunk.class);
							CloudService.this.exec(o, false, chunk);
						}
					});
				}
			}
		}
	}

	public Content lookupLocalContent(Hash addr) {
		for (var c : localContents) {
			if (c.hash.equals(addr)) {
				return c;
			}
		}

		return null;
	}

	public class ListChunksForFile extends TypedInnerOperation {

		public IntSet f(Hash addr) {
			IntSet s = new IntOpenHashSet();

			for (var c : localContents) {
				if (c.hash.equals(addr)) {
					s.addAll(c.chunks.keySet());
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
			Set<? extends Shape> shapes;
			IntSet chunkIndices;
		}

		public List<R> f(String owner) {
			var r = new ArrayList<R>();

			for (var content : localContents) {
				var shapes = content.owners.get(owner);

				if (shapes != null) {
					var i = new R();
					i.shapes = shapes;
					i.chunkIndices = content.chunks.keySet();
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
