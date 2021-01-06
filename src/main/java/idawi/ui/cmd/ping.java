package idawi.ui.cmd;

import java.util.List;

import idawi.ComponentDescriptor;
import idawi.Message;
import idawi.Service;
import idawi.service.PingPong;
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
	protected int work(Service localService, CommandLine cmdLine, double timeout)
			throws Throwable {
		int n = Integer.valueOf(getOptionValue(cmdLine, "--nbTimes"));
		boolean printIndividualPings = ! isOptionSpecified(cmdLine, "--hide");
		boolean progress = isOptionSpecified(cmdLine, "--progress");
		List<ComponentDescriptor> peers = ComponentDescriptor.fromPDL(cmdLine.findParameters());
		
		for (ComponentDescriptor p : peers) {
			Cout.info("pinging: " + p.toCDL());
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

			for (ComponentDescriptor p : peers) {
				if (printIndividualPings) {
					System.out.print(nbFailure + "/" + i + " ok. Pinging... ");
				}

				Message pong = localService.component.lookupService(PingPong.class).ping(p,
						timeout);

				if (pong == null) {
					if (printIndividualPings) {
						System.out.println("timeout");
					}

					++nbFailure;
					continue;
				}

				double duration = pong.receptionDate
						- ((Message) pong.content).route.last().emissionDate;

				if (printIndividualPings) {
					System.out.println("Pong after " + duration + "s");
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
