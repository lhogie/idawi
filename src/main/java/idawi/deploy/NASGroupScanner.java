package idawi.deploy;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;

import toools.extern.ProcesException;
import toools.io.file.Directory;
import toools.net.SSHParms;
import toools.net.SSHUtils;
import toools.thread.OneElementOneThreadProcessing;

public class NASGroupScanner {
	public static Set<Set<SSHParms>> groupByNAS(Collection<SSHParms> nodes, Consumer<String> feedback) {
		if (nodes.size() == 1) {
			Set<Set<SSHParms>> r = new HashSet<>();
			r.add(new HashSet<>(nodes));
			return r;
		}

		feedback.accept("Fetching distributed file systems among " + nodes);
		String dir = DeployerService.remoteClassDir + "detect-nas/";

		Directory localDir = new Directory(Directory.getHomeDirectory(),
				dir + "/" + InetAddress.getLoopbackAddress().getHostName());

		if (localDir.exists()) {
			localDir.deleteRecursively();
		}

		Vector<SSHParms> syncPeerList = new Vector<>(nodes);

		localDir.mkdirs();
		feedback.accept("marking NAS");
		new OneElementOneThreadProcessing<SSHParms>(new ArrayList<>(syncPeerList)) {

			@Override
			protected void process(SSHParms peer) {
				String filename = dir + peer.host.toString();

				try {
					SSHUtils.execShAndWait(peer, "mkdir -p " + filename);
				} catch (ProcesException e) {
					// e.printStackTrace();
					feedback.accept("discarding peer " + peer);
					syncPeerList.remove(peer);
				}
			}
		};

		Set<Set<SSHParms>> nasGroups = Collections.synchronizedSet(new HashSet<Set<SSHParms>>());

		feedback.accept("fetching marks");
		new OneElementOneThreadProcessing<SSHParms>(new ArrayList<>(syncPeerList)) {
			@Override
			protected void process(SSHParms peer) {
				try {
					nasGroups.add(new HashSet<>(SSHUtils.execShAndWait(peer, "ls " + dir).stream()
							.map(line -> findByName(line, nodes)).toList()));
				} catch (ProcesException e) {
					feedback.accept("discarding peer " + peer);
					syncPeerList.remove(peer);
				}
			}

			private SSHParms findByName(String hostname, Iterable<SSHParms> nodes) {
				for (var ssh : nodes) {
					if (ssh.host.equals(hostname)) {
						return ssh;
					}
				}

				throw new IllegalStateException(hostname);
			}
		};

		feedback.accept("removing marks");
		new OneElementOneThreadProcessing<Set<SSHParms>>(nasGroups) {
			@Override
			protected void process(Set<SSHParms> group) {
				for (var p : group) {
					try {
						SSHUtils.execShAndWait(p, "rm -rf " + dir);
						return;
					} catch (ProcesException e) {
						feedback.accept("discarding peer " + p);
						syncPeerList.remove(p);
					}
				}
			}
		};

		// if the local file has not be deleted by a peer on the same NAS
		if (localDir.exists()) {
			localDir.delete();
		}

		return nasGroups;
	}
}
