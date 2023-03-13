package idawi.deploy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;

import idawi.Component;
import idawi.InnerClassOperation;
import idawi.Service;
import idawi.knowledge_base.ComponentDescription;
import idawi.knowledge_base.ComponentRef;
import idawi.knowledge_base.MapService;
import idawi.messaging.MessageQueue;
import idawi.transport.PipeFromToChildProcess;
import idawi.transport.PipeFromToParentProcess;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.TransportService;
import toools.extern.ProcesException;
import toools.io.RSync;
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
	private static String remoteJREDir = "jre/";
	List<ComponentRef> failed = new ArrayList<>();

	public static class ExtraJVMDeploymentRequest implements Serializable {
		public ComponentRef target;
		public boolean suicideWhenParentDie = true;
		public Class<? extends Component> componentClass = Component.class;

		@Override
		public String toString() {
			return "deploying " + target;
		}
	}

	public static class RemoteDeploymentRequest extends ExtraJVMDeploymentRequest {
		public SSHParms ssh = new SSHParms();
		public double timeoutInSecond;

		public static List<RemoteDeploymentRequest> from(Collection<ComponentRef> a) {
			return a.stream().map(p -> {
				var r = new RemoteDeploymentRequest();
				r.target = p;
				return r;

			}).toList();
		}

		@Override
		public String toString() {
			return super.toString() + " via " + ssh;
		}
	}

	public DeployerService(Component peer) {
		super(peer);

		if (!remoteClassDir.endsWith("/"))
			throw new IllegalStateException("class dir should end with a '/': " + remoteClassDir);

		registerOperation(new remote_deploy());
		registerOperation(new local_deploy());
		registerOperation(new deploy_in_other_jvms());
	}

	@Override
	public String getFriendlyName() {
		return "deployer";
	}

	public class remote_deploy extends InnerClassOperation {
		@Override
		public void impl(MessageQueue in) throws Throwable {
			var trigger = in.poll_sync();
			var reqs = (Collection<RemoteDeploymentRequest>) trigger.content;

			System.err.println(reqs);
			deploy(reqs, stdout -> reply(trigger, stdout), stderr -> reply(trigger, stderr),
					peerOk -> reply(trigger, peerOk));

			System.err.println("done");
		}

		@Override
		public String getDescription() {
			return "deploy components using SSH";
		}
	}

	public class local_deploy extends InnerClassOperation {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var msg = in.poll_sync();
			var req = (Collection<ComponentRef>) msg.content;
			List<Component> compoennts = new ArrayList<>();
			deployInThisJVM(req, peerOk -> compoennts.add(peerOk));
			reply(msg, req.size() + " compoennts created");
			SharedMemoryTransport.chain(compoennts, SharedMemoryTransport.class);
			reply(msg, "chained");
		}

		@Override
		public String getDescription() {
			return "deploy new components in the current JVM";
		}
	}

	public class deploy_in_other_jvms extends InnerClassOperation {
		@Override
		public String getDescription() {
			return "deploy in new JVMs";
		}

		@Override
		public void impl(MessageQueue q) throws Throwable {
			var trigger = q.poll_sync();
			var reqs = (Collection<ExtraJVMDeploymentRequest>) trigger.content;
			deploy(reqs, line -> reply(trigger, line), ok -> reply(trigger, ok));
		}

	}

	public void deploy(Collection<ExtraJVMDeploymentRequest> reqs, Consumer<String> feedback,
			Consumer<ComponentRef> peerOk) throws IOException {
		for (var req : reqs) {
			deploy(req, feedback);
			peerOk.accept(req.target);
		}
	}

	public void deploy(ExtraJVMDeploymentRequest req, Consumer<String> feedback) throws IOException {
		String java = System.getProperty("java.home") + "/bin/java";
		String classpath = System.getProperty("java.class.path");
		LongProcess startingNode = new LongProcess("starting new JVM", null, -1, line -> feedback.accept(line));
		Process p = Runtime.getRuntime()
				.exec(new String[] { java, "-cp", classpath, DeployerService.class.getName(), "run_by_a_node" });
		feedback.accept("sending node startup info");
		initChildProcess(p, req, feedback);
		startingNode.end();
	}

	private void initChildProcess(Process proc, ExtraJVMDeploymentRequest req, Consumer<String> feedback)
			throws IOException {
		DeployInfo deployInfo = new DeployInfo();
		deployInfo.req = req;
		deployInfo.parent = component.ref();

		OutputStream out = proc.getOutputStream();
		TransportService.serializer.write(deployInfo, out);
		out.flush();

		var pipesToChildren = component.lookup(PipeFromToChildProcess.class);
		var e = pipesToChildren.add(req.target, proc);

		feedback.accept("waiting for " + req.target + " to be ready");
		Object response = e.waitForChild.poll_sync(10);

		if (response == null) { // timeout :(
			throw new IllegalStateException("timeout");
		} else if (response instanceof ComponentDescription) { // deploy success
			component.knowledgeBase().considers((ComponentDescription) response);
			component.forEachServiceOfClass(MapService.class,
					s -> s.bidi(component.ref(), deployInfo.req.target, pipesToChildren.getClass()));
		} else if (response instanceof Throwable) { // deploy error
			throw new IllegalStateException(req.target + " failed", (Throwable) response);
		} else {
			throw new IllegalStateException("obtaining " + response);
		}
	}

	public List<Component> deployInThisJVM(Collection<ComponentRef> reqs) {
		return deployInThisJVM(reqs, c -> {
		});
	}

	public List<Component> deployInThisJVM(Object... ids) {
		var l = Arrays.stream(ids).map(i -> new ComponentRef(i)).toList();
		return deployInThisJVM(l);
	}

	public List<Component> deployInThisJVM(Collection<ComponentRef> reqs, Consumer<Component> peerOk) {
		var r = new ArrayList<Component>();

		for (var req : reqs) {
			Component t = new Component(req);
			t.parent = component.ref();
			component.lookup(SharedMemoryTransport.class).connectTo(t);

			if (peerOk != null) {
				peerOk.accept(t);
			}

			r.add(t);
		}

		return r;
	}

	public void deploy(Collection<RemoteDeploymentRequest> peers, Consumer<String> feedback, Consumer<String> stderr,
			Consumer<ComponentRef> peerOk) throws IOException {

		// identifies the set of peers that have filesystem in common
		var nasGroups = groupByNAS(peers, feedback);
		feedback.accept("found NAS groups: " + nasGroups);

		new OneElementOneThreadProcessing<Set<RemoteDeploymentRequest>>(nasGroups) {

			@Override
			protected void process(Set<RemoteDeploymentRequest> nasGroup) throws IOException {
				// picks a random peer in the group
				RemoteDeploymentRequest n = nasGroup.iterator().next();

				// sends the binaries there using rsync over SSH
				rsyncBinaries(n.ssh, feedback, stderr);

				// starts it on every nodes, in parallel
				startJVMs(nasGroup, feedback);
			}
		};

	}

	private void rsyncBinaries(SSHParms ssh, Consumer<String> rsyncOut, Consumer<String> rsyncErr) throws IOException {
		ClassPath.retrieveSystemClassPath().rsyncTo(ssh, remoteClassDir, rsyncOut, rsyncErr);
		RSync.rsyncTo(ssh, List.of(Directory.getHomeDirectory().getChildDirectory("jre")), remoteJREDir, rsyncOut,
				rsyncErr);
	}

	private void startJVMs(Set<RemoteDeploymentRequest> nasGroup, Consumer<String> feedback) {
		new OneElementOneThreadProcessing<RemoteDeploymentRequest>(nasGroup) {

			@Override
			protected void process(RemoteDeploymentRequest req) throws IOException {
				LongProcess startingNode = new LongProcess("starting node in JVM on " + req.target + " via SSH", null,
						-1, line -> feedback.accept(line));
				Process p = SSHUtils.exec(req.ssh, "jre/jdk-$(uname -s)-$(uname -m)/bin/java", "-cp",
						remoteClassDir + ":$(echo " + remoteClassDir + "* | tr ' ' :)", DeployerService.class.getName(),
						"run_by_a_node");

				feedback.accept("sending node startup info to " + req);

				initChildProcess(p, req, feedback);
				startingNode.end();
			}
		};
	}

	private static class DeployInfo implements Serializable {
		ExtraJVMDeploymentRequest req;
		ComponentRef parent;
//		Set<CR> peersSharingFileSystem;
	}

	// this method should be called only by this class
	public static void main(String[] args)
			throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		if (args.length != 1 || !args[0].equals("run_by_a_node")) {
			System.err.println(
					"this main class is not intended to be run  by a user: it has to called by the deployment service");
			System.exit(1);
		}

		try {

			System.out.println("JVM " + System.getProperty("java.vendor") + " " + System.getProperty("java.version")
					+ " is running");
			System.out.println("Now reading deployment information");
			var deployInfo = (DeployInfo) TransportService.serializer.read(System.in);

			System.out.println("instantiating component");
			var child = Clazz.makeInstance(deployInfo.req.componentClass.getConstructor(ComponentRef.class),
					deployInfo.req.target);
			child.parent = deployInfo.parent;
//			child.otherComponentsSharingFilesystem.addAll(deployInfo.peersSharingFileSystem);

			var pipe = new PipeFromToParentProcess(child, child.parent, deployInfo.req.suicideWhenParentDie);
			child.forEachServiceOfClass(MapService.class, s -> s.bidi(child.ref(), deployInfo.parent, pipe.getClass()));

			// tell the parent process that this node is ready for connections
			System.out.println("ready, notifying parent");
			PipeFromToParentProcess.sysout(child.descriptor());
			System.out.flush();

			// prevents the JVM to quit
			while (true) {
				Threads.sleep(1);
//				PipeFromToParentProcess.sysout(child.descriptor());
			}
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.err.println("Stopping JVM");
			PipeFromToParentProcess.sysout(t);
			System.out.flush();
			System.err.flush();
			Threads.sleep(1);
			System.exit(1);
		}
	}

	public Set<ComponentRef> findNASGroupOf(Set<Set<ComponentRef>> nasGroups, ComponentRef n) {
		for (Set<ComponentRef> g : nasGroups) {
			if (g.contains(n)) {
				return g;
			}
		}

		return null;
	}

	public static Set<Set<RemoteDeploymentRequest>> groupByNAS(Collection<RemoteDeploymentRequest> nodes,
			Consumer<String> feedback) {

		if (nodes.size() == 1) {
			Set<Set<RemoteDeploymentRequest>> r = new HashSet<>();
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

		Vector<RemoteDeploymentRequest> syncPeerList = new Vector<>(nodes);

		localDir.mkdirs();
		feedback.accept("marking NAS");
		new OneElementOneThreadProcessing<RemoteDeploymentRequest>(new ArrayList<>(syncPeerList)) {

			@Override
			protected void process(RemoteDeploymentRequest peer) {
				String filename = dir + peer.target.toString();

				try {
					SSHUtils.execShAndWait(peer.ssh, "mkdir -p " + filename);
				} catch (ProcesException e) {
					// e.printStackTrace();
					feedback.accept("discarding peer " + peer);
					syncPeerList.remove(peer);
				}
			}
		};

		Set<Set<RemoteDeploymentRequest>> nasGroups = Collections
				.synchronizedSet(new HashSet<Set<RemoteDeploymentRequest>>());

		feedback.accept("fetching marks");
		new OneElementOneThreadProcessing<RemoteDeploymentRequest>(new ArrayList<>(syncPeerList)) {
			@Override
			protected void process(RemoteDeploymentRequest peer) {
				Set<RemoteDeploymentRequest> s = new HashSet<>();

				try {
					List<String> stdout = SSHUtils.execShAndWait(peer.ssh, "ls " + dir);

					for (var line : stdout) {
						s.add(findByName(line, nodes));
					}

					nasGroups.add(s);
				} catch (ProcesException e) {
					feedback.accept("discarding peer " + peer);
					syncPeerList.remove(peer);
				}
			}

			private RemoteDeploymentRequest findByName(String name, Iterable<RemoteDeploymentRequest> nodes) {
				for (RemoteDeploymentRequest p : nodes) {
					if (p.target.ref.equals(name)) {
						return p;
					}
				}

				throw new IllegalStateException(name);
			}
		};

		feedback.accept("removing marks");
		new OneElementOneThreadProcessing<Set<RemoteDeploymentRequest>>(nasGroups) {
			@Override
			protected void process(Set<RemoteDeploymentRequest> group) {
				for (var p : group) {
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

}
