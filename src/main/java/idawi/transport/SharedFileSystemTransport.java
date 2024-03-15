package idawi.transport;

import java.util.List;
import java.util.Random;
import java.util.Set;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import toools.exceptions.NotYetImplementedException;
import toools.io.Cout;
import toools.io.file.Directory;
import toools.io.file.RegularFile;
import toools.math.MathsUtilities;
import toools.thread.Threads;

public class SharedFileSystemTransport extends TransportService {
	final Directory baseDirectory;
	private final Directory inboxDirectory;

	public SharedFileSystemTransport(Component c, String peerName, Directory baseDirectory) {
		super(c);

		this.baseDirectory = baseDirectory;
		this.inboxDirectory = new Directory(baseDirectory, peerName);

		if (inboxDirectory.exists()) {
			inboxDirectory.clear();
		} else {
			inboxDirectory.mkdirs();
		}

		Cout.info("monitoring directory " + inboxDirectory + " for message files");

		Threads.newThread_loop_periodic(1000, () -> true, () -> {
			List<RegularFile> files = inboxDirectory.listRegularFiles();
			files.sort((a, b) -> Long.compare(b.getAgeMs(), a.getAgeMs()));

			files.forEach(f -> {
				Message msg = extract(f);
				processIncomingMessage(msg);
				f.delete();
			});
		});
	}

	@Override
	protected void sendImpl(Message msg) {
		var to = msg.route.last().link.dest.component;
		String filename = String.valueOf(Math.abs(new Random().nextLong()));
		Directory toDir = new Directory(baseDirectory, to.toString());
		toDir.ensureExists();
		RegularFile f = new RegularFile(toDir, filename + ".ser");
		byte[] bytes = component.secureSerializer.toBytes(msg);
		f.setContent(bytes);
	}

	@Override
	public String getName() {
		return "shared-directory driver";
	}


	protected Message extract(RegularFile f) {
		try {
			Message msg = (Message) component.secureSerializer.fromBytes(f.getContent());
			return msg;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public void dispose(Link l) {
	}
	
	@Override
	public double latency() {
		return MathsUtilities.pickRandomBetween(0.1, 1, Idawi.prng);
	}

}
