package idawi.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Graph;
import idawi.Service;
import idawi.To;
import idawi.TransportLayer;
import idawi.net.LMI;
import idawi.net.NetworkingService;
import idawi.net.PipeFromToChildProcess;
import idawi.net.PipeFromToParentProcess;
import toools.extern.ProcesException;
import toools.io.Cout;
import toools.io.file.Directory;
import toools.net.SSHParms;
import toools.net.SSHUtils;
import toools.progression.LongProcess;
import toools.reflect.ClassPath;
import toools.reflect.Clazz;
import toools.thread.OneElementOneThreadProcessing;
import toools.thread.Threads;

public class ComponentDeployer extends Service {
	List<ComponentInfo> failed = new ArrayList<>();
	String remoteClassDir = "jthing.classes/";

	public static class DeploymentRequest implements Serializable {
		public Collection<ComponentInfo> peers;
		public double timeoutInSecond;
		public boolean suicideWhenParentDie;
		public boolean printRsync;
	}

	public static class LocalDeploymentRequest implements Serializable {
		public int n;
		public boolean suicideWhenParentDie;
	}

	@Override
	public String getFriendlyName() {
		return "deployer";
	}

	public ComponentDeployer(Component peer) {
		super(peer);

		// receives deployment requests from other peers
		registerOperation("deploy", (msg, out) -> {
			DeploymentRequest req = (DeploymentRequest) msg.content;
			deploy(req.peers, req.suicideWhenParentDie, req.timeoutInSecond, req.printRsync,
					stdout -> out.accept(stdout), peerOk -> out.accept(peerOk));
		});

		registerOperation("local_deploy", (msg, out) -> {
			LocalDeploymentRequest req = (LocalDeploymentRequest) msg.content;
			List<Component> things = new ArrayList<>();
			deployLocalPeers(req.n, req.suicideWhenParentDie, peerOk -> things.add(peerOk));
			out.accept(req.n + " things created");
			LMI.chain(things);
			out.accept("chained");
		});

		registerOperation("deploy_in_other_jvm", (msg, out) -> {
			Graph deploymentPlan = (Graph) msg.content;
			apply(deploymentPlan, 1, true, stdout -> out.accept(stdout), peerOk -> out.accept(peerOk));
		});

	}

	public void apply(Graph deploymentPlan, double timeoutInSecond, boolean printRsync, Consumer<Object> feedback,
			Consumer<ComponentInfo> peerOk) throws IOException {
		Set<ComponentInfo> toDeploy = deploymentPlan.get(component.descriptor());
		deploy(toDeploy, true, timeoutInSecond, printRsync, feedback, peerOk);

		To to = new To();
		to.notYetReachedExplicitRecipients = toDeploy;
		to.service = ComponentDeployer.class;
		to.operationOrQueue = "d3";
		send(deploymentPlan, to).collect();
	}

	public List<Component> deploy(Collection<ComponentInfo> peers, boolean suicideWhenParentDie, double timeoutInSecond,
			boolean printRsync, Consumer<Object> feedback, Consumer<ComponentInfo> peerOk) throws IOException {

		Collection<ComponentInfo> inThisJVM = findThoseForThisJVM(peers);
		List<Component> localThings = new ArrayList<>();
		deployLocalPeers(inThisJVM, suicideWhenParentDie, okThing -> {
			localThings.add(okThing);
			peerOk.accept(okThing.descriptor());
		});

		Set<ComponentInfo> remotePeers = toools.collections.Collections.difference(peers, inThisJVM);

		if (!remotePeers.isEmpty()) {
			deployRemote(remotePeers, suicideWhenParentDie, timeoutInSecond, printRsync, feedback, peerOk);
		}

		return localThings;
	}

	public void deployOtherJVM(ComponentInfo d, boolean suicideWhenParentDie, Consumer<Object> feedback,
			Consumer<ComponentInfo> peerOk) throws IOException {
		String java = System.getProperty("java.home") + "/bin/java";
		String classpath = System.getProperty("java.class.path");

		LongProcess startingNode = new LongProcess("starting new JVM", null, -1, line -> feedback.accept(line));
		Process p = Runtime.getRuntime()
				.exec(new String[] { java, "-cp", classpath, ComponentDeployer.class.getName() });

		feedback.accept("sending node startup info");
		init(p, d, Set.of(component.descriptor()), suicideWhenParentDie, feedback);
		startingNode.end();
	}

	private void init(Process p, ComponentInfo d, Set<ComponentInfo> peersSharingFS, boolean suicideWhenParentDie,
			Consumer<Object> feedback) throws IOException {
		OutputStream out = p.getOutputStream();
		DeployInfo deployInfo = new DeployInfo();
		deployInfo.id = d;
		deployInfo.suicideWhenParentDies = suicideWhenParentDie;
		deployInfo.parent = component.descriptor();
		deployInfo.peersSharingFileSystem = peersSharingFS;
		TransportLayer.serializer.write(deployInfo, out);
		out.flush();

		PipeFromToChildProcess childPipe = new PipeFromToChildProcess(d, p);
		childPipe.setNewMessageConsumer(component.lookupService(NetworkingService.class));
		NetworkingService network = component.lookupService(NetworkingService.class);
		network.transport.addProtocol(childPipe);
		network.transport.peer2protocol.put(d, childPipe);

		feedback.accept("waiting for " + d + " to be ready");
		String response = (String) childPipe.waitForChild.get_blocking(10000);

		if (response == null) {
			throw new IllegalStateException("timeout");
		} else if (response.equals(PipeFromToChildProcess.started)) {
			return;
		} else if (response.equals(PipeFromToChildProcess.failed)) {
			throw new IllegalStateException(d + " failed");
		} else {
			throw new IllegalStateException();
		}
	}

	public Set<Component> deployLocalPeers(int n, boolean suicideWhenParentDie, Consumer<Component> peerOk) {
		Set<Component> s = new HashSet<>();

		for (int i = 0; i < n; ++i) {
			Component t = new Component();
			deployLocalPeer(t, suicideWhenParentDie);
			peerOk.accept(t);
			s.add(t);
		}

		return s;
	}

	public void deployLocalPeers(Collection<ComponentInfo> localPeers, boolean suicideWhenParentDie,
			Consumer<Component> peerOk) {
		for (ComponentInfo d : localPeers) {
			Component t = new Component(d);
			deployLocalPeer(t, suicideWhenParentDie);
			peerOk.accept(t);
		}
	}

	private void deployLocalPeer(Component newThing, boolean suicideWhenParentDie) {
		if (suicideWhenParentDie) {
			component.killOnDeath.add(newThing);
		}

		LMI.connect(component, newThing);
	}

	public void deployRemote(Collection<ComponentInfo> peers, boolean suicideWhenParentDie, double timeoutInSecond,
			boolean printRsync, Consumer<Object> feedback, Consumer<ComponentInfo> peerOk) throws IOException {

		// identifies the set of peers that have filesystem in common
		Set<Set<ComponentInfo>> nasGroups = groupByNAS(peers, feedback);
		feedback.accept("found NAS groups: " + nasGroups);

		// rsync the classes to the remote groups and start the nodes
		new OneElementOneThreadProcessing<Set<ComponentInfo>>(nasGroups) {

			@Override
			protected void process(Set<ComponentInfo> nasGroup) throws Throwable {
				// picks a random peer in the group
				ComponentInfo n = nasGroup.iterator().next();

				// sends the binaries there
				rsync(n.sshParameters);

				// makes sure the JVM is okay for all the nodes in the group
				ensureCompliantJVM(n.sshParameters);

				// starts it on every nodes, in parallel
				startJVM(nasGroup, feedback);
			}

			private void ensureCompliantJVM(SSHParms n) {
				feedback.accept("Local JRE is " + System.getProperty("java.version"));
				feedback.accept("test if JDK 14 is installed on " + n);
				List<String> stdout = SSHUtils.execShAndWait(n,
						"if test -f jdk-14.0.1/bin/java; then echo yes; else echo no; fi");
				final AtomicBoolean jdk14 = new AtomicBoolean(stdout.get(0).equals("yes"));

				if (!jdk14.get()) {
					Threads.newThread_loop(1000, () -> !jdk14.get(),
							() -> feedback.accept("Downloading and installing JDK 14 on " + n));
					SSHUtils.execShAndWait(n,
							"wget https://download.java.net/java/GA/jdk14.0.1/664493ef4a6946b186ff29eb326336a2/7/GPL/openjdk-14.0.1_linux-x64_bin.tar.gz && tar xzf openjdk-14.0.1_linux-x64_bin.tar.gz");
					jdk14.set(true);
				}
			}

			private void rsync(SSHParms ssh) throws IOException {
				LongProcess rsyncing = new LongProcess("rsync to " + ssh, null, -1, line -> feedback.accept(line));
				Consumer<String> rsyncOut = l -> {
					if (printRsync) {
						rsyncing.stdout((String) l);
					}
				};

				int exitCode = ClassPath.retrieveSystemClassPath().rsyncTo(ssh, ssh.hostname, remoteClassDir,
						l -> rsyncOut.accept(l), l -> rsyncOut.accept(l));
				rsyncing.end();

				if (exitCode != 0) {
					feedback.accept("Error: rsync to " + ssh + " exited with value " + exitCode);
				}
			}

			private void startJVM(Set<ComponentInfo> nasGroup, Consumer<Object> feedback) {
				new OneElementOneThreadProcessing<ComponentInfo>(peers) {

					@Override
					protected void process(ComponentInfo peer) {
						try {
							LongProcess startingNode = new LongProcess("starting node in JVM on " + peer + " via SSH",
									null, -1, line -> feedback.accept(line));
							Process p = SSHUtils.exec(peer.sshParameters, "jdk-14.0.1/bin/java", "-cp",
									remoteClassDir + ":$(echo " + remoteClassDir + "* | tr ' ' :)",
									ComponentDeployer.class.getName());

							feedback.accept("sending node startup info to " + peer);

							init(p, peer, nasGroup, suicideWhenParentDie, feedback);
						} catch (Exception e) {
							feedback.accept(e);
						}
					}
				};
			}
		};
	}

	private Collection<ComponentInfo> findThoseForThisJVM(Collection<ComponentInfo> peers) {
		Collection<ComponentInfo> r = new HashSet<>();

		for (ComponentInfo p : peers) {
			if (p.isLocalhost()) {
				r.add(p);
			}
		}

		return r;
	}

	private Collection<ComponentInfo> findThoseForAnotherJVMInTheSameComputer(Collection<ComponentInfo> peers) {
		Collection<ComponentInfo> r = new HashSet<>();

		for (ComponentInfo p : peers) {
			if (p.isLocalhost()) {
				r.add(p);
			}
		}

		return r;
	}

	public static class DeployInfo implements Serializable {
		ComponentInfo id;
		boolean suicideWhenParentDies;
		ComponentInfo parent;
		Set<ComponentInfo> peersSharingFileSystem;
	}

	// this method should be called only by this class
	public static void main(String[] args)
			throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		try {
			Cout.raw_stdout.println("JVM started, now reading deployment information");
			Object o = null;

			while (!((o = TransportLayer.serializer.read(System.in)) instanceof DeployInfo)) {
				Cout.warning("ignoring object of class " + o.getClass().getName());
			}

			// Cout.raw_stdout.println("deployment info: " + o);
			DeployInfo deployInfo = (DeployInfo) o;

			Cout.raw_stdout.println("instantiating thing");
			Component t = (Component) Clazz.makeInstance(Component.class.getConstructor(ComponentInfo.class),
					deployInfo.id);

			t.descriptorRegistry.add(deployInfo.parent);
			t.otherComponentsSharingFilesystem.addAll(deployInfo.peersSharingFileSystem);

			t.lookupService(NetworkingService.class).transport
					.addProtocol(new PipeFromToParentProcess(deployInfo.parent, deployInfo.suicideWhenParentDies));

			// tell the parent process that this node is ready for connections
			Cout.raw_stdout.println("ready, notifying parent");
			Cout.raw_stdout.println(PipeFromToChildProcess.started);
			Cout.raw_stdout.flush();

			// prevents the JVM to quit
			Threads.sleepForever();
		} catch (Throwable t) {
			t.printStackTrace(Cout.raw_stdout);
			Cout.raw_stdout.println(PipeFromToChildProcess.failed);
			Cout.raw_stdout.flush();
			System.exit(1);
		}
	}

	public Set<ComponentInfo> findNASGroupOf(Set<Set<ComponentInfo>> nasGroups, ComponentInfo n) {
		for (Set<ComponentInfo> g : nasGroups) {
			if (g.contains(n)) {
				return g;
			}
		}

		return null;
	}

	public Set<Set<ComponentInfo>> groupByNAS(Collection<ComponentInfo> nodes, Consumer<Object> feedback) {

		if (nodes.size() == 1) {
			Set<Set<ComponentInfo>> r = new HashSet<>();
			r.add(new HashSet<>(nodes));
			return r;
		}

		feedback.accept("Fetching distributed file systems among " + nodes);
		String dir = remoteClassDir + "detect-nas/";

		Directory localDir = new Directory(Directory.getHomeDirectory(),
				dir + "/" + InetAddress.getLoopbackAddress().getHostName());

		if (localDir.exists()) {
			localDir.deleteRecursively();
		}

		Vector<ComponentInfo> syncPeerList = new Vector<>(nodes);

		localDir.mkdirs();
		feedback.accept("marking NAS");
		new OneElementOneThreadProcessing<ComponentInfo>(new ArrayList<>(syncPeerList)) {

			@Override
			protected void process(ComponentInfo peer) {
				String filename = dir + peer.friendlyName;

				try {
					SSHUtils.execShAndWait(peer.sshParameters, "mkdir -p " + filename);
				} catch (ProcesException e) {
					// e.printStackTrace();
					feedback.accept("discarding peer " + peer);
					syncPeerList.remove(peer);
				}
			}
		};

		Set<Set<ComponentInfo>> nasGroups = Collections.synchronizedSet(new HashSet<Set<ComponentInfo>>());

		feedback.accept("fetching marks");
		new OneElementOneThreadProcessing<ComponentInfo>(new ArrayList<>(syncPeerList)) {
			@Override
			protected void process(ComponentInfo peer) {
				Set<ComponentInfo> s = new HashSet<>();

				try {
					List<String> stdout = SSHUtils.execShAndWait(peer.sshParameters, "ls " + dir);
					stdout.forEach(line -> s.add(findByName(line, nodes)));
					nasGroups.add(s);
				} catch (ProcesException e) {
					feedback.accept("discarding peer " + peer);
					syncPeerList.remove(peer);
				}
			}

			private ComponentInfo findByName(String name, Iterable<ComponentInfo> nodes) {
				for (ComponentInfo p : nodes) {
					if (p.friendlyName.equals(name)) {
						return p;
					}
				}

				throw new IllegalStateException(name);
			}
		};

		feedback.accept("removing marks");
		new OneElementOneThreadProcessing<Set<ComponentInfo>>(nasGroups) {
			@Override
			protected void process(Set<ComponentInfo> group) {
				for (ComponentInfo p : group) {
					try {
						SSHUtils.execShAndWait(p.sshParameters, "rm -rf " + dir);
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

	public Set<ComponentInfo> pickOneNodeInEveryNASGroup(Set<Set<ComponentInfo>> nasGroups, Random r) {
		Set<ComponentInfo> s = new HashSet<>();

		for (Set<ComponentInfo> g : nasGroups) {
			s.add(g.iterator().next());
		}

		return s;
	}

}
