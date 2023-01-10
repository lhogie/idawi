package idawi.ui.cmd;

import idawi.ProcessContentFile;
import j4u.CommandLine;
import toools.io.Cout;
import toools.io.file.RegularFile;
import toools.net.SSHParms;
import toools.net.SSHUtils;

public class lsrp extends Command {
	public static void main(String[] args) throws Throwable {
		new lsrp(null).run(args);
	}

	public lsrp(RegularFile launcher) {
		super(launcher);
	}

	@Override
	public String getShortDescription() {
		return "list running processes";
	}

	@Override
	public int runScript(CommandLine cmdLine) throws Throwable {

		for (String sshHost : cmdLine.findParameters()) {
			SSHParms sshparms = new SSHParms();
			sshparms.host = sshHost;
			SSHUtils.execShAndWait(sshparms,
					"if [ ! -d $HOME/.pafadipo/running/ ]; then exit 0; fi; for FILENAME in $HOME/.pafadipo/running/*; do AGE=$(($(date +%s) - $(date +%s -r $FILENAME))); if [ $AGE -lt 1 ]; then echo $FILENAME; fi; done");
		}

		for (RegularFile f : ProcessContentFile.processDirectory.listRegularFiles()) {
			if (f.getAgeMs() < 1000) {
				Cout.result(f.getName());
			}
			else {
				f.delete();
			}
		}

		return 0;
	}
}
