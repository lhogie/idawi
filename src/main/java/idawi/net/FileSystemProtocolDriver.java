package idawi.net;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import idawi.ComponentInfo;
import idawi.Message;
import idawi.TransportLayer;
import toools.io.Cout;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.thread.Threads;

public class FileSystemProtocolDriver extends TransportLayer {
	final Directory baseDirectory;
	private final Directory inboxDirectory;
	private final MessageBuiltNeighborhood peers;

	public FileSystemProtocolDriver(String peerName, Directory baseDirectory) {

		this.baseDirectory = baseDirectory;
		this.inboxDirectory = new Directory(baseDirectory, peerName);
		peers = new MessageBuiltNeighborhood(this);

		if (inboxDirectory.exists()) {
			inboxDirectory.clear();
		}
		else {
			inboxDirectory.mkdirs();
		}
	}

	@Override
	public Collection<ComponentInfo> neighbors() {
		return peers.peers();
	}

	@Override
	public void send(Message msg, Collection<ComponentInfo> neighbors) {
		for (ComponentInfo n : neighbors) {
			String filename = String.valueOf(Math.abs(new Random().nextLong()));
			Directory toDir = new Directory(baseDirectory, n.toString());
			toDir.ensureExists();
			RegularFile f = new RegularFile(toDir, filename + ".ser");
			byte[] bytes = serializer.toBytes(msg);
			f.setContent(bytes);
		}
	}

	@Override
	public String getName() {
		return "shared-directory driver";
	}

	@Override
	public boolean canContact(ComponentInfo c) {
		return c.friendlyName != null;
	}

	@Override
	public void injectLocalInfoTo(ComponentInfo c) {
		c.inboxDirectory = inboxDirectory;
	}

	private boolean run = false;

	@Override
	protected void start() {
		Cout.info("monitoring directory " + inboxDirectory + " for message files");

		run = true;

		Threads.newThread_loop_periodic(1000, () -> run, () -> {
			List<RegularFile> files = inboxDirectory.listRegularFiles();
			files.sort((a, b) -> Long.compare(b.getAgeMs(), a.getAgeMs()));

			files.forEach(f -> {
				Message msg = extract(f);
				peers.messageJustReceivedFrom(msg.route.last().component);
				processIncomingMessage(msg);
				f.delete();
			});
		});
	}

	protected Message extract(RegularFile f) {
		try {
			Message msg = (Message) serializer.fromBytes(f.getContent());
			return msg;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void stop() {
		run = false;
	}
}
