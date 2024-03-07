package idawi.deploy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
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
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.service.SystemService;
import idawi.service.local_view.ComponentInfo;
import idawi.service.local_view.LocalViewService;
import idawi.service.local_view.ServiceInfo;
import idawi.transport.Link;
import idawi.transport.PipeFromToParentProcess;
import idawi.transport.PipesFromToChildrenProcess;
import idawi.transport.PipesFromToChildrenProcess.Entry;
import idawi.transport.SharedMemoryTransport;
import idawi.transport.Topologies;
import toools.extern.ProcesException;
import toools.io.RSync;
import toools.io.file.Directory;
import toools.io.ser.JavaSerializer;
import toools.net.SSHParms;
import toools.net.SSHUtils;
import toools.progression.LongProcess;
import toools.reflect.ClassPath;
import toools.thread.OneElementOneThreadProcessing;
import toools.thread.Threads;

public class DeployerService extends Service {
	private static String remoteClassDir = Service.class.getPackageName() + ".classpath/";
	private static String remoteJREDir = "jre/";
	List<Component> failed = new ArrayList<>();

	public static class DeploymentRequest implements Serializable {
		public Component target;

		@Override
		public String toString() {
			return "deploying " + target;
		}
	}

	public static class ExtraJVMDeploymentRequest extends DeploymentRequest {
		Component parent;
	}

	public static class RemoteDeploymentRequest extends ExtraJVMDeploymentRequest {
		public SSHParms ssh = new SSHParms();
		public double timeoutInSecond;

		public static List<RemoteDeploymentRequest> from(Collection<Component> a) {
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
	}

	@Override
	public String getFriendlyName() {
		return "deployer";
	}

	private void remote_deploy_impl(MessageQueue in) throws Throwable {
		var trigger = in.poll_sync();
		var reqs = (Collection<RemoteDeploymentRequest>) trigger.content;

		System.err.println(reqs);
		deployRemotely(reqs, stdout -> reply(trigger, stdout, false), stderr -> reply(trigger, stderr, false),
				peerOk -> reply(trigger, peerOk, false));

		System.err.println("done");
	}

	public String remote_deploy_impl_description() {
		return "deploy components using SSH";
	}

	public class local_deploy extends InnerClassEndpoint {

		@Override
		public void impl(MessageQueue in) throws Throwable {
			var msg = in.poll_sync();
			var req = (Collection<Component>) msg.content;
			List<Component> components = new ArrayList<>();
			deployInThisJVM(req, peerOk -> components.add(peerOk));
			reply(msg, req.size() + " components created", false);
			Topologies.chain(components, (a, b) -> SharedMemoryTransport.class, components);
			reply(msg, "chained", true);
		}

		@Override
		public String getDescription() {
			return "deploy new components in the current JVM";
		}
	}

	public class deploy_in_other_jvms extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "deploy in new JVMs";
		}

		@Override
		public void impl(MessageQueue q) throws Throwable {
			var trigger = q.poll_sync();
			var reqs = (Collection<ExtraJVMDeploymentRequest>) trigger.content;
			deployInNewJVMs(reqs, line -> reply(trigger, line, false), ok -> reply(trigger, ok, false));
		}

	}

	public void deployInNewJVMs(Collection<ExtraJVMDeploymentRequest> reqs, Consumer<String> feedback,
			Consumer<Component> peerOk) throws IOException {
		for (var req : reqs) {
			deployInNewJVM(req, feedback);
			peerOk.accept(req.target);
		}
	}

	public void deployInNewJVM(ExtraJVMDeploymentRequest req, Consumer<String> feedback) throws IOException {
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
		req.parent = component;

		OutputStream out = proc.getOutputStream();

		var pipesToChildren = component.service(PipesFromToChildrenProcess.class);
		Entry childEntry = pipesToChildren.add(req.target, proc);

		pipesToChildren.component.serializer.write(req, out);
		out.flush();

		feedback.accept("waiting for " + req.target + " to be ready");
		Object response = childEntry.waitForChild.poll_sync(10);
		// no more responses will be accepted
		childEntry.waitForChild = null;

		if (response == null) { // timeout :(
			throw new IllegalStateException("timeout");
		} else if (response instanceof Throwable) { // deploy error
			throw new IllegalStateException(req.target + " failed", (Throwable) response);
		} else if (response instanceof ComponentInfo) {
			var lv = component.service(LocalViewService.class);
			var connectionToParent = childEntry.child.service(PipeFromToParentProcess.class);
			lv.g.markLinkActive(new Link(pipesToChildren, connectionToParent));
			lv.g.markLinkActive(new Link(connectionToParent, pipesToChildren));
			childEntry.child.dt().update((ComponentInfo) response);
		} else {
			throw new IllegalStateException("obtaining " + response);
		}
	}

	public List<Component> deployInThisJVM(Collection<Component> reqs) {
		return deployInThisJVM(reqs, c -> {
		});
	}

	public List<Component> deployInThisJVM(String... ids) {
		var l = Arrays.stream(ids).map(i -> new Component()).toList();
		return deployInThisJVM(l);
	}

	public List<Component> deployInThisJVM(Collection<Component> twins, Consumer<Component> peerOk) {
		var r = new ArrayList<Component>();

		for (var twin : twins) {
			var realComponent = new Component();
			realComponent.deployer = component;
			var from = component.service(SharedMemoryTransport.class, true);
			var to = realComponent.service(SharedMemoryTransport.class, true);
			component.localView().g.markLinkActive(to, from);
			component.localView().g.markLinkActive(from, to);
			realComponent.localView().g.markLinkActive(to, from);
			realComponent.localView().g.markLinkActive(from, to);

			if (peerOk != null) {
				peerOk.accept(realComponent);
			}

			r.add(realComponent);
		}

		return r;
	}

	public class remote_deploy extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "deploy on another hosts";
		}

		@Override
		public void impl(MessageQueue q) throws Throwable {
			var trigger = q.poll_sync();
			var reqs = (Collection<RemoteDeploymentRequest>) trigger.content;
			deployRemotely(reqs, line -> reply(trigger, line, false), line -> reply(trigger, "stderr: " + line, false),
					ok -> reply(trigger, ok, false));
		}
	}

	public void deployRemotely(Collection<RemoteDeploymentRequest> peers, Consumer<String> feedback,
			Consumer<String> stderr, Consumer<Component> peerOk) throws IOException {

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

	private static void rsyncBinaries(SSHParms ssh, Consumer<String> rsyncOut, Consumer<String> rsyncErr)
			throws IOException {
		ClassPath.retrieveSystemClassPath().rsyncTo(ssh, remoteClassDir, rsyncOut, rsyncErr);

		var d = jreDir();

		if (d == null)
			throw new IllegalStateException(
					System.getProperty("java.specification.version") + "Java can't be found in $HOME/jre/");

		RSync.rsyncTo(ssh, List.of(Directory.getHomeDirectory().getChildDirectory("jre")), remoteJREDir, rsyncOut,
				rsyncErr);
	}

	public static class JRE {
		final int[] version;
		final String arch;
		final String os;

		public JRE(Directory d) {
			var e = d.getName().split(" ");
			version = Arrays.stream(e[1].split("\\.")).mapToInt(s -> Integer.valueOf(s)).toArray();
			this.os = e[2];
			this.arch = e[3];
		}
	}

	private static List<JRE> jreDir() {
		return new Directory("$HOME/jre/").listDirectories().stream().map(d -> new JRE(d)).toList();
	}

	private void startJVMs(Set<RemoteDeploymentRequest> nasGroup, Consumer<String> feedback) {
		new OneElementOneThreadProcessing<RemoteDeploymentRequest>(nasGroup) {

			@Override
			protected void process(RemoteDeploymentRequest req) throws IOException {
				LongProcess startingNode = new LongProcess("starting node in JVM on " + req.target + " via SSH", null,
						-1, line -> feedback.accept(line));
				var remoteJREDir = "jre/jdk " + System.getProperty("java.specification.version")
						+ " $(uname -s) $(uname -m)";
				Process p = SSHUtils.exec(req.ssh, remoteJREDir + "/bin/java", "-cp",
						remoteClassDir + ":$(echo " + remoteClassDir + "* | tr ' ' :)", DeployerService.class.getName(),
						"run_by_a_node");

				feedback.accept("sending node startup info to " + req);

				initChildProcess(p, req, feedback);
				startingNode.end();
			}
		};
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
			var req = (ExtraJVMDeploymentRequest) new JavaSerializer<>().read(System.in);

			System.out.println("instantiating component");
			var child = req.target.getClass().getConstructor(String.class).newInstance(req.target.publicKey());
			child.addBasicServices();
			child.deployer = req.parent;
			req.parent.turnToDigitalTwin(child);
//			child.otherComponentsSharingFilesystem.addAll(deployInfo.peersSharingFileSystem);

			// create the pipe to the parent
			var pipeToParent = new PipeFromToParentProcess(child, req.parent);
			pipeToParent.suicideIfLoseParent = !req.target.autonomous;

			// updates the twin parent
			var parentInterface = req.parent.service(PipesFromToChildrenProcess.class);
			child.localView().g.markLinkActive(new Link(pipeToParent, parentInterface));
			child.localView().g.markLinkActive(new Link(parentInterface, pipeToParent));

			// tell the parent process that this node is ready for connections
			System.out.println("ready, notifying parent");
			pipeToParent.send(child.service(SystemService.class).localComponentInfo());
			System.out.flush();

			// prevents the JVM to quit
			while (true) {
				Threads.sleep(1);
//				PipeFromToParentProcess.sysout(child.descriptor());
			}
		} catch (Throwable err) {
			err.printStackTrace(System.err);
			System.err.println("Stopping JVM");
			PipeFromToParentProcess.sendBytes(new JavaSerializer<>().toBytes(err));
			System.out.flush();
			System.err.flush();
			Threads.sleep(1);
			System.exit(1);
		}
	}

	private static Set<Set<RemoteDeploymentRequest>> groupByNAS(Collection<RemoteDeploymentRequest> nodes,
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
					if (p.target.friendlyName.equals(name)) {
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

	public void apply(ComponentInfo d) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {

		for (ServiceInfo sd : d.services) {
			if (component.service(sd.clazz) == null) {
				var s = sd.clazz.getConstructor(Component.class).newInstance(component);
				s.apply(sd);
			}
		}
	}

}
