package idawi.ui.cmd;

import idawi.Component;
import idawi.messaging.Message;
import j4u.CommandLine;
import j4u.CommandLineSpecification;
import toools.io.Cout;
import toools.progression.LongProcess;

public class ping extends CommunicatingCommand {
	public static void main(String[] args) throws Throwable {
		new ping().run(args);
	}

	@Override
	protected void specifyCmdLine(CommandLineSpecification spec) {
		spec.addOption("--nbTimes", "-n", "[0-9]+", 1, "nb of pings");
		spec.addOption("--hide", null, null, null, "do not print each individual ping/pong");
		spec.addOption("--progress", "-p", null, null, "print progress statistics");
	}

	@Override
	protected int work(Component localComponent, CommandLine cmdLine, double timeout) throws Throwable {
		int n = Integer.valueOf(getOptionValue(cmdLine, "--nbTimes"));
		boolean printIndividualPings = !isOptionSpecified(cmdLine, "--hide");
		boolean progress = isOptionSpecified(cmdLine, "--progress");
		var peers = cmdLine.findParameters().stream().map(s -> {
			var c = new Component();
			c.friendlyName = s;
			return c;
		}).toList();

		for (var p : peers) {
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

			for (var p : peers) {
				if (printIndividualPings) {
					System.out.print(nbFailure + "/" + i + " ok. Pinging... ");
				}

				Message pong = localComponent.bb().ping(p).poll_sync();

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
	public String getDescription() {
		return null;
	}
}
