package idawi.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import idawi.Component;
import idawi.Service;
import toools.extern.Proces;
import toools.io.file.RegularFile;

public class OAR extends Service {

	public static final String jobName = "Jthing";

	public OAR(Component peer) {
		super(peer);
		registerEndpoint("submit", (msg, out) -> {
			String requirements = (String) msg.content;
			submit(requirements);
		});

		registerEndpoint("submit", (msg, returns) -> {
			String requirements = (String) msg.content;
			submit(requirements);
		});
	}

	@Override
	public String getFriendlyName() {
		return "OAR";
	}

	private static int retrieveJobID() {
		for (String line : new String(Proces.exec("oarstat")).split("\n")) {
			if (line.contains(jobName)) {
				return new Scanner(line).nextInt();
			}
		}

		return -1;
	}

	public static int submit(String req) {
		byte[] stdout = Proces.exec("oarsub", req, "--name", jobName, "cat");

		Pattern p = Pattern.compile("OAR_JOB_ID=([0-9]+)");

		for (String line : new String(stdout).split("\n")) {
			System.out.println(line);
			Matcher m = p.matcher(line);

			// if we find a match, get the group
			if (m.find()) {
				// we're only looking for one group, so get it
				String theGroup = m.group(1);
				return Integer.valueOf(theGroup);
			}
		}

		throw new IllegalStateException();
	}

	private static class OARJob {

		private final int id;
		private final RegularFile stdoutFile, stderrFile;

		public OARJob(int id) {
			this.id = id;
			stdoutFile = new RegularFile(jobName + "-" + id + ".stdout");
			stderrFile = new RegularFile(jobName + "-" + id + ".stderr");
		}

		public void delete() {
			Proces.exec("oardel", "" + id);
		}

		public String getStdOut() {
			return stdoutFile.getContentAsText();
		}

		public String getStdErr() {
			return stderrFile.getContentAsText();
		}

		public int getID() {
			return id;
		}

		public void streamFile(RegularFile f, Consumer<String> out) {
			try {
				Process p = Runtime.getRuntime().exec("tail -n +1 -f " + f.getPath());

				BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));

				while (true) {
					String s = is.readLine();

					if (s == null)
						break;

					out.accept(s);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void waitForStdIOFiles() {
			Proces.exec("while ! test -f '" + stderrFile.getPath() + "'; do sleep 1; done");
		}

		public Properties retrieveInfo() {
			try {
				Properties p = new Properties();
				p.load(new ByteArrayInputStream(Proces.exec("oarstat -f -j " + id)));
				return p;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
}
