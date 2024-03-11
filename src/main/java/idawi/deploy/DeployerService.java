package idawi.deploy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.InnerClassEndpoint;
import idawi.Service;
import idawi.messaging.MessageQueue;
import idawi.transport.PipesFromToChildrenProcess;
import toools.io.RSync;
import toools.io.file.Directory;
import toools.net.SSHParms;
import toools.net.SSHUtils;
import toools.progression.LongProcess;
import toools.reflect.ClassPath;
import toools.thread.OneElementOneThreadProcessing;

public class DeployerService extends Service {
	public static String remoteClassDir = Service.class.getPackageName() + ".classpath/";
	private static String remoteJREDir = "jre/";
	List<Component> failed = new ArrayList<>();

	public DeployerService(Component peer) {
		super(peer);

		if (!remoteClassDir.endsWith("/"))
			throw new IllegalStateException("class dir should end with a '/': " + remoteClassDir);
	}

	@Override
	public String getFriendlyName() {
		return "deployer";
	}

	public String remote_deploy_impl_description() {
		return "deploy components using SSH";
	}

	public static class DeployError extends Error {
		public DeployError(Throwable cause) {
			super(cause);
		}
	}

	public Component newLocalJVM() {
		String java = System.getProperty("java.home") + "/bin/java";
		String classpath = System.getProperty("java.class.path");
		try {
			Process p = Runtime.getRuntime()
					.exec(new String[] { java, "-cp", classpath, RemoteMain.class.getName(), "run_by_a_node" });
			return component.service(PipesFromToChildrenProcess.class, true).add(p).f();
		} catch (Throwable err) {
			throw new DeployError(err);
		}
	}

	public void deployViaSSH(Collection<SSHParms> peers, Consumer<String> feedback, Consumer<String> stderr,
			Consumer<Component> ok, Consumer<Throwable> errors) throws IOException {

		// identifies the set of peers that have filesystem in common
		var nasGroups = NASGroupScanner.groupByNAS(peers, feedback);
		feedback.accept(
				"found NAS groups: " + nasGroups.stream().map(g -> g.stream().map(r -> r.host).toList()).toList());

		new OneElementOneThreadProcessing<Set<SSHParms>>(nasGroups) {

			@Override
			protected void process(Set<SSHParms> nasGroup) throws IOException {
				// picks a random peer in the group
				var n = nasGroup.iterator().next();

				// sends the binaries there using rsync over SSH
				rsyncBinaries(n, feedback, stderr);

				// starts it on every nodes, in parallel
				startJVMs(nasGroup, feedback, ok, errors);
			}
		};

	}

	private static void rsyncBinaries(SSHParms ssh, Consumer<String> rsyncOut, Consumer<String> rsyncErr)
			throws IOException {
		ClassPath.retrieveSystemClassPath().rsyncTo(ssh, remoteClassDir, rsyncOut, rsyncErr);

		RSync.rsyncTo(ssh, List.of(Directory.getHomeDirectory().getChildDirectory("jre")), remoteJREDir, rsyncOut,
				rsyncErr);
	}

	private void startJVMs(Set<SSHParms> hosts, Consumer<String> feedback, Consumer<Component> ok,
			Consumer<Throwable> errors) {
		new OneElementOneThreadProcessing<SSHParms>(hosts) {

			@Override
			protected void process(SSHParms req) throws IOException {
				LongProcess startingNode = new LongProcess("starting node in JVM on " + req + " via SSH", null, -1,
						line -> feedback.accept(line));

				// use any JVM with the same spec as the depoyer
				Process p = SSHUtils.exec(req,
						"jre/jdk-" + System.getProperty("java.specification.version") + "*" + "-$(uname -s)-$(uname -m)"
								+ "/bin/java",
						"-cp", remoteClassDir + ":$(echo " + remoteClassDir + "* | tr ' ' :)",
						DeployerService.class.getName(), "run_by_a_node");

				feedback.accept("sending node startup info to " + req);

				try {
					ok.accept(component.service(PipesFromToChildrenProcess.class, true).add(p).f());
				} catch (Throwable err) {
					errors.accept(err);
				}
			}
		};
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

	private static List<JRE> listJREs() {
		return new Directory("$HOME/jre/").listDirectories().stream().map(d -> new JRE(d)).toList();
	}

	public class remote_deploy extends InnerClassEndpoint {
		@Override
		public String getDescription() {
			return "deploy on another hosts";
		}

		@Override
		public void impl(MessageQueue q) throws Throwable {
			var trigger = q.poll_sync();
			var reqs = (Collection<SSHParms>) trigger.content;
			deployViaSSH(reqs, line -> reply(trigger, line, false), line -> reply(trigger, new Error(line), false),
					ok -> reply(trigger, ok, false), err -> reply(trigger, err, false));
		}
	}
}
