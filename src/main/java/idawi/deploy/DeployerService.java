package idawi.deploy;

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
import idawi.ComponentDescriptor;
import idawi.InnerOperation;
import idawi.MessageQueue;
import idawi.RegistryService;
import idawi.Service;
import idawi.net.LMI;
import idawi.net.NetworkingService;
import idawi.net.PipeFromToChildProcess;
import idawi.net.PipeFromToParentProcess;
import idawi.net.TransportLayer;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import toools.extern.ProcesException;
import toools.io.Cout;
import toools.io.Utilities;
import toools.io.file.Directory;
import toools.net.SSHParms;
import toools.net.SSHUtils;
import toools.progression.LongProcess;
import toools.reflect.ClassPath;
import toools.reflect.Clazz;
import toools.thread.OneElementOneThreadProcessing;
import toools.thread.Threads;

public class DeployerService extends Service {
	private static String remoteClassDir = Service.class.getPackageName() + ".classpath/";
	List<ComponentDescriptor> failed = new ArrayList<>();

	public static class DeploymentParameters implements Serializable {
		public Collection<ComponentDescriptor> targets = new HashSet<>();
		public double timeoutInSecond;
		public boolean suicideWhenParentDie;
	}

	public static class LocalDeploymentRequest implements Serializable {
		public int n;
		public boolean suicideWhenParentDie;
	}

	public DeployerService(Component peer) {
		super(peer);

		if (!remoteClassDir.endsWith("/"))
			throw new IllegalStateException("class dir should end with a '/': " + remoteClassDir);

		registerOperation(new deploy());
		registerOperation(new local_deploy());
		registerOperation(new deploy_in_other_jvm());
	}

	@Override
	public String getFriendlyName() {
		return "deployer";
	}

	public class deploy extends InnerOperation {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var trigger = in.poll_sync();
			DeploymentParameters req = (DeploymentParameters) trigger.content;
			System.err.println(req.targets);
			deploy(req.targets, req.suicideWhenParentDie, req.timeoutInSecond, rsyncOut -> reply(trigger, rsyncOut),
					rsyncErr -> reply(trigger, rsyncErr), stdout -> reply(trigger, stdout),
					peerOk -> reply(trigger, peerOk));
			System.err.println("done");
		}

		@Override
		public String getDescription() {
			return "deploy";
		}
	}

	public class local_deploy extends InnerOperation {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var msg = in.poll_sync();
			LocalDeploymentRequest req = (LocalDeploymentRequest) msg.content;
			List<Component> compoennts = new ArrayList<>();
			deployInThisJVM(req.n, i -> "component-" + i, req.suicideWhenParentDie, peerOk -> compoennts.add(peerOk));
			reply(msg, req.n + " compoennts created");
			LMI.chain(compoennts);
			reply(msg, "chained");
		}

		@Override
		public String getDescription() {
			return "deploy new components in the current JVM";
		}
	}

	public class deploy_in_other_jvm extends InnerOperation {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var msg = in.poll_sync();
			var deploymentGraph = (DeploymentPlan) msg.content;
			apply(deploymentGraph, 1, rsyncOut -> reply(msg, rsyncOut), rsyncErr -> reply(msg, rsyncErr),
					stdout -> reply(msg, stdout), peerOk -> reply(msg, peerOk));
		}

		@Override
		public String getDescription() {
			return "deploy";
		}
	}

	public class deploy_in_other_jvms extends InnerOperation {

		@Override
		public String getDescription() {
			return "deploy in new JVMs";
		}

		@Override
		public void impl(MessageQueue q) throws Throwable {
			var trigger = q.poll_sync();
			var dd = (List<ComponentDescriptor>) trigger.content;
			deployInNewJVMs(dd, line -> reply(trigger, line), ok -> reply(trigger, ok));
		}
	}

	public void apply(DeploymentPlan deploymentGraph, double timeoutInSecond, Consumer<String> rsyncOut,
			Consumer<String> rsyncErr, Consumer<Object> feedback, Consumer<ComponentDescriptor> peerOk)
			throws IOException {
		Set<ComponentDescriptor> toDeploy = deploymentGraph.g.getSuccessors(component.descriptor());
		deploy(toDeploy, true, timeoutInSecond, rsyncOut, rsyncErr, feedback, peerOk);

		// var to = new OperationAddress(toDeploy, DeployerService.d3);
		// start(to, true, deploymentPlan).returnQ.collect();
	}

	public List<Component> deploy(Collection<ComponentDescriptor> peers, boolean suicideWhenParentDie,
			double timeoutInSecond, Consumer<String> rsyncOut, Consumer<String> rsyncErr, Consumer<Object> feedback,
			Consumer<ComponentDescriptor> peerOk) throws IOException {

		Collection<ComponentDescriptor> inThisJVM = findLocalhosts(peers);
		List<Component> localThings = new ArrayList<>();
		deployInThisJVM(inThisJVM, suicideWhenParentDie, okThing -> {
			localThings.add(okThing);

			if (peerOk != null) {
				peerOk.accept(okThing.descriptor());
			}
		});

		Set<ComponentDescriptor> remotePeers = toools.collections.Collections.difference(peers, inThisJVM);

		if (!remotePeers.isEmpty()) {
			deployRemote(remotePeers, suicideWhenParentDie, timeoutInSecond, rsyncOut, rsyncErr, feedback, peerOk);
		}

		return localThings;
	}

	public void deployInNewJVMs(Collection<ComponentDescriptor> dd, Consumer<Object> messages,
			Consumer<ComponentDescriptor> ok) throws IOException {
		var threads = new ArrayList<Thread>();

		for (var d : dd) {
			threads.add(new Thread(() -> {
				try {
					deployOtherJVM(d, true, messages, ok);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}));
		}

		threads.forEach(t -> t.start());
		threads.forEach(t -> {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	public void deployOtherJVM(ComponentDescriptor d, boolean suicideWhenParentDie, Consumer<Object> feedback,
			Consumer<ComponentDescriptor> peerOk) throws IOException {
		String java = System.getProperty("java.home") + "/bin/java";
		String classpath = System.getProperty("java.class.path");

		LongProcess startingNode = new LongProcess("starting new JVM", null, -1, line -> feedback.accept(line));

		Process p = Runtime.getRuntime().exec(new String[] { java, "-cp", classpath, DeployerService.class.getName() });

		feedback.accept("sending node startup info");
		initChildProcess(p, d, Set.of(component.descriptor()), suicideWhenParentDie, feedback);
		peerOk.accept(d);
		startingNode.end();
	}

	private void initChildProcess(Process proc, ComponentDescriptor childDescriptor,
			Set<ComponentDescriptor> peersSharingFS, boolean suicideWhenParentDie, Consumer<Object> feedback)
			throws IOException {
		DeployInfo deployInfo = new DeployInfo();
		deployInfo.id = childDescriptor;
		deployInfo.suicideWhenParentDies = suicideWhenParentDie;
		deployInfo.parent = component.descriptor();
		deployInfo.peersSharingFileSystem = peersSharingFS;

		OutputStream out = proc.getOutputStream();
		TransportLayer.serializer.write(deployInfo, out);
		out.flush();

		PipeFromToChildProcess childPipe = new PipeFromToChildProcess(component, childDescriptor, proc);
		var network = component.lookup(NetworkingService.class);
		network.transport.addTransport(childPipe);
		network.transport.add(childDescriptor, childPipe);

		feedback.accept("waiting for " + childDescriptor + " to be ready");
		String response = (String) childPipe.waitForChild.poll_sync(10000);

		if (response == null) {
			throw new IllegalStateException("timeout");
		} else if (response.equals(PipeFromToChildProcess.started)) {
			return;
		} else if (response.equals(PipeFromToChildProcess.failed)) {
			throw new IllegalStateException(childDescriptor + " failed");
		} else {
			throw new IllegalStateException();
		}
	}

	public Set<Component> deployInThisJVM(int n, Int2ObjectFunction<String> i2name, boolean suicideWhenParentDie,
			Consumer<Component> peerOk) {
		Set<Component> s = new HashSet<>();

		for (int i = 0; i < n; ++i) {
			Component t = new Component(i2name.apply(i));
			deployInThisJVM(t, suicideWhenParentDie);

			if (peerOk != null) {
				peerOk.accept(t);
			}

			s.add(t);
		}

		return s;
	}

	public void deployInThisJVM(Collection<ComponentDescriptor> localPeers, boolean suicideWhenParentDie,
			Consumer<Component> peerOk) {
		for (ComponentDescriptor d : localPeers) {
			Component t = new Component(d);
			deployInThisJVM(t, suicideWhenParentDie);

			if (peerOk != null) {
				peerOk.accept(t);
			}
		}
	}

	private void deployInThisJVM(Component newThing, boolean suicideWhenParentDie) {
		newThing.parent = component.descriptor();

		if (suicideWhenParentDie) {
			component.killOnDeath.add(newThing);
		}

		LMI.connect(component, newThing);
	}

	public void deployRemote(Collection<ComponentDescriptor> peers, boolean suicideWhenParentDie,
			double timeoutInSecond, Consumer<String> rsyncOut, Consumer<String> rsyncErr, Consumer<Object> feedback,
			Consumer<ComponentDescriptor> peerOk) throws IOException {

		// identifies the set of peers that have filesystem in common
		Set<Set<ComponentDescriptor>> nasGroups = groupByNAS(peers, feedback);
		feedback.accept("found NAS groups: " + nasGroups);

		// rsync the classes to the remote groups and start the nodes
		new OneElementOneThreadProcessing<Set<ComponentDescriptor>>(nasGroups) {

			@Override
			protected void process(Set<ComponentDescriptor> nasGroup) throws Throwable {
				// picks a random peer in the group
				ComponentDescriptor n = nasGroup.iterator().next();

				// sends the binaries there
				rsyncBinaries(n.ssh, rsyncOut, rsyncErr);

				// makes sure the JVM is okay for all the nodes in the group
				ensureCompliantJVM(n.ssh);

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
							"wget https://download.java.net/java/GA/jdk14.0.1/664493ef4a6946b186ff29eb326336a2/7/GPL/openjdk-14.0.1_linux-x64_bin.tar.gz && tar xzf openjdk-14.0.1_linux-x64_bin.tar.gz && rm -f openjdk-14.0.1_linux-x64_bin.tar.gz");
					jdk14.set(true);
				}
			}

			private void rsyncBinaries(SSHParms ssh, Consumer<String> rsyncOut, Consumer<String> rsyncErr)
					throws IOException {
				LongProcess rsyncing = new LongProcess("rsync to " + ssh, null, -1, line -> feedback.accept(line));
				int exitCode = ClassPath.retrieveSystemClassPath().rsyncTo(ssh, ssh.host, remoteClassDir,
						l -> rsyncOut.accept(l), l -> rsyncErr.accept(l));
				rsyncing.end();

				if (exitCode != 0) {
					feedback.accept("Error: rsync to " + ssh + " exited with value " + exitCode);
				}
			}

			private int rsyncResources(Directory resourceDir, SSHParms sshParameters, String remotePath,
					Consumer<String> stdout, Consumer<String> stderr) throws IOException {
				LongProcess rsyncing = new LongProcess("rsync to " + sshParameters, null, -1,
						line -> feedback.accept(line));
				List<String> args = new ArrayList<>();
				args.add("rsync");

				if (sshParameters != null) {
					args.add("-e");
					List<String> ssh = new ArrayList<>();
					ssh.add(SSHUtils.sshCmd());
					SSHUtils.addSSHOptions(ssh, sshParameters);
					args.add(toools.collections.Collections.toString(ssh, " "));
				}

				args.add("-a");
				args.add("--delete");
				args.add("--copy-links");
				args.add("-v");

				args.add(resourceDir.getPath() + "/");
				args.add(sshParameters.host + ":" + remotePath + "/");

				try {
					// System.out.println(args);
					Process rsync = Runtime.getRuntime().exec(args.toArray(new String[0]));
					Utilities.grabLines(rsync.getInputStream(), stdout, err -> {
					});
					Utilities.grabLines(rsync.getErrorStream(), stderr, err -> {
					});
					rsync.waitFor();
					rsyncing.end();
					return rsync.exitValue();
				} catch (InterruptedException e1) {
					throw new IllegalStateException(e1);
				}
			}

			private void startJVM(Set<ComponentDescriptor> nasGroup, Consumer<Object> feedback) {
				new OneElementOneThreadProcessing<ComponentDescriptor>(peers) {

					@Override
					protected void process(ComponentDescriptor peer) {

						// sends the resouces there
						// rsyncResources(n.sshParameters);

						try {
							LongProcess startingNode = new LongProcess("starting node in JVM on " + peer + " via SSH",
									null, -1, line -> feedback.accept(line));
							Process p = SSHUtils.exec(peer.ssh, "jdk-14.0.1/bin/java", "-cp",
									remoteClassDir + ":$(echo " + remoteClassDir + "* | tr ' ' :)",
									DeployerService.class.getName());

							feedback.accept("sending node startup info to " + peer);

							initChildProcess(p, peer, nasGroup, suicideWhenParentDie, feedback);
							startingNode.end();
						} catch (Exception e) {
							feedback.accept(e);
						}
					}
				};
			}
		};
	}

	private static Collection<ComponentDescriptor> findLocalhosts(Collection<ComponentDescriptor> peers) {
		Collection<ComponentDescriptor> r = new HashSet<>();

		for (ComponentDescriptor p : peers) {
			if (p.isLocalhost()) {
				r.add(p);
			}
		}

		return r;
	}

	public static class DeployInfo implements Serializable {
		ComponentDescriptor id;
		boolean suicideWhenParentDies;
		ComponentDescriptor parent;
		Set<ComponentDescriptor> peersSharingFileSystem;
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
			var deployInfo = (DeployInfo) o;

			Cout.raw_stdout.println("instantiating component");
			var child = (Component) Clazz.makeInstance(Component.class.getConstructor(ComponentDescriptor.class),
					deployInfo.id);
			child.parent = deployInfo.parent;

			child.lookupOperation(RegistryService.add.class).f(child.parent);
			child.otherComponentsSharingFilesystem.addAll(deployInfo.peersSharingFileSystem);

			var network = child.lookup(NetworkingService.class);
			var pipe = new PipeFromToParentProcess(child, child.parent, deployInfo.suicideWhenParentDies);
			network.transport.addTransport(pipe);
			network.transport.add(child.parent, pipe);

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

	public Set<ComponentDescriptor> findNASGroupOf(Set<Set<ComponentDescriptor>> nasGroups, ComponentDescriptor n) {
		for (Set<ComponentDescriptor> g : nasGroups) {
			if (g.contains(n)) {
				return g;
			}
		}

		return null;
	}

	public static Set<Set<ComponentDescriptor>> groupByNAS(Collection<ComponentDescriptor> nodes,
			Consumer<Object> feedback) {

		if (nodes.size() == 1) {
			Set<Set<ComponentDescriptor>> r = new HashSet<>();
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

		Vector<ComponentDescriptor> syncPeerList = new Vector<>(nodes);

		localDir.mkdirs();
		feedback.accept("marking NAS");
		new OneElementOneThreadProcessing<ComponentDescriptor>(new ArrayList<>(syncPeerList)) {

			@Override
			protected void process(ComponentDescriptor peer) {
				String filename = dir + peer.name;

				try {
					SSHUtils.execShAndWait(peer.ssh, "mkdir -p " + filename);
				} catch (ProcesException e) {
					// e.printStackTrace();
					feedback.accept("discarding peer " + peer);
					syncPeerList.remove(peer);
				}
			}
		};

		Set<Set<ComponentDescriptor>> nasGroups = Collections.synchronizedSet(new HashSet<Set<ComponentDescriptor>>());

		feedback.accept("fetching marks");
		new OneElementOneThreadProcessing<ComponentDescriptor>(new ArrayList<>(syncPeerList)) {
			@Override
			protected void process(ComponentDescriptor peer) {
				Set<ComponentDescriptor> s = new HashSet<>();

				try {
					List<String> stdout = SSHUtils.execShAndWait(peer.ssh, "ls " + dir);

					for (var line : stdout) {
						ComponentDescriptor c = findByName(line, nodes);
						s.add(c);
					}

					nasGroups.add(s);
				} catch (ProcesException e) {
					feedback.accept("discarding peer " + peer);
					syncPeerList.remove(peer);
				}
			}

			private ComponentDescriptor findByName(String name, Iterable<ComponentDescriptor> nodes) {
				for (ComponentDescriptor p : nodes) {
					if (p.name.equals(name)) {
						return p;
					}
				}

				throw new IllegalStateException(name);
			}
		};

		feedback.accept("removing marks");
		new OneElementOneThreadProcessing<Set<ComponentDescriptor>>(nasGroups) {
			@Override
			protected void process(Set<ComponentDescriptor> group) {
				for (ComponentDescriptor p : group) {
					try {
						SSHUtils.execShAndWait(p.ssh, "rm -rf " + dir);
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

	public Set<ComponentDescriptor> pickOneNodeInEveryNASGroup(Set<Set<ComponentDescriptor>> nasGroups, Random r) {
		Set<ComponentDescriptor> s = new HashSet<>();

		for (Set<ComponentDescriptor> g : nasGroups) {
			s.add(g.iterator().next());
		}

		return s;
	}

}
