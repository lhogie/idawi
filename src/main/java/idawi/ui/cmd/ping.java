package idawi.ui.cmd;

import java.util.List;

import idawi.Component;
import idawi.knowledge_base.ComponentRef;
import idawi.messaging.Message;
import j4u.CommandLine;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.progression.LongProcess;

public class ping extends CommunicatingCommand {
	public static void main(String[] args) throws Throwable {
		new ping(null).run(args);
	}

	public ping(RegularFile launcher) {
		super(launcher);
		addOption("--nbTimes", "-n", "[0-9]+", 1, "nb of pings");
		addOption("--hide", null, null, null, "do not print each individual ping/pong");
		addOption("--progress", "-p", null, null, "print progress statistics");
	}

	@Override
	protected int work(Component c, CommandLine cmdLine, double timeout) throws Throwable {
		int n = Integer.valueOf(getOptionValue(cmdLine, "--nbTimes"));
		boolean printIndividualPings = !isOptionSpecified(cmdLine, "--hide");
		boolean progress = isOptionSpecified(cmdLine, "--progress");
		List<ComponentRef> peers = cmdLine.findParameters().stream().map(s -> new ComponentRef(s)).toList();

		for (ComponentRef p : peers) {
			Cout.info("pinging: " + p);
		}
		int nbFailure = 0;

		LongProcess lp = null;

		if (progress) {
			lp = new LongProcess("ping/pong", "ping", n);
		}

		for (int i = 0; i < n; ++i) {

			if (progress) {
				lp.sensor.progressStatus++;
				lp.temporaryResult = nbFailure + " failures on " + i + " attempts";
			}

			for (ComponentRef p : peers) {
				if (printIndividualPings) {
					System.out.print(nbFailure + "/" + i + " ok. Pinging... ");
				}

				Message pong = c.bb().ping(p).poll_sync();

				if (pong == null) {
					if (printIndividualPings) {
						System.out.println("timeout");
					}

					++nbFailure;
					continue;
				}

				if (printIndividualPings) {
					System.out.println("Pong after " + pong.route.duration() + "s");
				}
			}
		}

		if (progress) {
			lp.end();
		}

		return nbFailure;
	}

	@Override
	public String getShortDescription() {
		return "ping";
	}
}
