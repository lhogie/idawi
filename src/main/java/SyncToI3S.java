import java.io.IOException;

import toools.net.SSHParms;
import toools.reflect.ClassPath;

public class SyncToI3S {
	public static void main(String[] args) throws IOException {
		syncToI3S();
	}
	
	public static void syncToI3S() throws IOException {
		var ssh = new SSHParms();
		ssh.host = "bastion.i3s.unice.fr";
		ssh.username = "hogie";
		String dir = "/net1/home/hogie/public_html/software/idawi/last_bins/";
		ClassPath.retrieveSystemClassPath().rsyncTo(ssh, dir, out -> System.out.println(out),
				err -> System.err.println(err));
		System.out.println(ssh.host + ":" + dir);
	}
}
