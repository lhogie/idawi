package idawi;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import idawi.net.UDPDriver;
import toools.io.file.Directory;
import toools.net.NetUtilities;
import toools.net.SSHParms;
import toools.text.TextUtilities;
import toools.util.Date;

/**
 * Specifies how to reach a given peer.
 * 
 * @author lhogie
 *
 */
public class ComponentInfo implements Serializable {
	enum WHERE {
		here, new_jvm, ssh
	}

	public double date = Date.time();
	public String friendlyName;
	public SSHParms sshParameters = new SSHParms();
	public Directory inboxDirectory;
	public final List<InetAddress> inetAddresses = new ArrayList<>();
	public Integer tcpPort, udpPort;
	public final List<String> webServers = new ArrayList<>();
	public WHERE where;
//	public Set<Class<? extends Service>> services = new HashSet<>();
	public Set<String> services = new HashSet<>();
	// private int isLocalhost;

	// final Map<String, Object> map = new HashMap<>();
	
	public Set<String> neighbors = new HashSet<>();

	public static class Neighbor implements Serializable{
		public String id;
		public double latency;
		public int rate;
	}
	
	@Override
	public String toString() {
		String s = friendlyName.toString();
		return s;
	}

	public String toTDL() {
		Properties props = new Properties();
		props.put("name", friendlyName);
		props.put("ip", TextUtilities.concat(", ", inetAddresses, ip -> ip.getHostName()));

		if (tcpPort != null) {
			props.put("tcp_port", "" + tcpPort);
		}

		if (udpPort != null) {
			props.put("udp_port", "" + udpPort);
		}

		if (sshParameters != null) {
			if (sshParameters.hostname != null) {
				props.put("ssh.host", sshParameters.hostname);
			}

			if (sshParameters.username != null) {
				props.put("ssh.user", sshParameters.username);
			}

			props.put("ssh.port", "" + sshParameters.port);
		}

		StringWriter w = new StringWriter();

		try {
			props.store(w, "this is a peer description");
			return w.toString();
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	public interface PDLHandler {
		void f(ComponentInfo p, String value) throws CDLException;
	}

	static Map<String, PDLHandler> key_pdlHandlerMap = new HashMap<>();

	static {
		key_pdlHandlerMap.put("name", (p, v) -> p.friendlyName = v);
		key_pdlHandlerMap.put("tcp_port", (p, v) -> p.tcpPort = Integer.valueOf(v));
		key_pdlHandlerMap.put("udp_port", (p, v) -> p.udpPort = Integer.valueOf(v));
		key_pdlHandlerMap.put("ssh", (p, v) -> p.sshParameters = SSHParms.fromSSHString(v));
		key_pdlHandlerMap.put("where", (p, v) -> p.where = WHERE.valueOf(v));
		key_pdlHandlerMap.put("ip", (p, v) -> {
			for (String i : v.split(",")) {
				try {
					p.inetAddresses.add(InetAddress.getByName(i));
				} catch (UnknownHostException e) {
					new CDLException(e);
				}
			}
		});
	}

	public static ComponentInfo fromCDL(String s) {

		ComponentInfo peer = new ComponentInfo();

		s = s.replaceAll(" +", " ");
		s = s.replaceAll(" */ *", "\n");
		Properties props = new Properties();

		try {
			props.load(new StringReader(s));
		} catch (IOException e1) {
			throw new CDLException(e1);
		}

		for (Entry<Object, Object> e : props.entrySet()) {
			PDLHandler h = key_pdlHandlerMap.get(e.getKey());

			if (h == null) {
				throw new CDLException("not supported: " + e.getKey());
			}

			h.f(peer, e.getValue().toString());
		}

		// try to find an IP address
		if (peer.inetAddresses.isEmpty()) {
			if (peer.sshParameters.hostname == null) {
				try {
					peer.inetAddresses.add(InetAddress.getLocalHost());
				} catch (UnknownHostException e1) {
					// no network on that computer, not too important
				}
			} else {
				try {
					peer.inetAddresses.add(InetAddress.getByName(peer.sshParameters.hostname));
				} catch (UnknownHostException e) {
					// the ssh host does not correspond to anything in the DNS
				}
			}
		}

		if (peer.friendlyName == null) {
			if (!peer.inetAddresses.isEmpty()) {
				peer.friendlyName = peer.inetAddresses.get(0).getHostName();
			} else if (peer.sshParameters.hostname != null) {
				peer.friendlyName = peer.sshParameters.hostname;
			} else {
				peer.friendlyName = "thing-" + ThreadLocalRandom.current().nextInt();
			}
		}

		return peer;
	}

	public String toHTML() {
		String s = "<ul>";
		s += "\n\t<li>id: " + friendlyName;
		s += "\n\t<li>IPs: " + inetAddresses;

		if (sshParameters != null) {
			s += "\n\t<li>SSH port: " + sshParameters.port;
		}

		if (tcpPort != null) {
			s += "\n\t<li>TCP port: " + tcpPort;
		}

		if (udpPort != null) {
			s += "\n\t<li>UDP port: " + udpPort;
		}

		if (sshParameters != null) {
			s += "\n\t<li>SSH parameters: " + udpPort;
			s += "\n\t<ul>\n";
			s += "\n\t\t<li>username: " + sshParameters.username;
			s += "\n\t\t<li>port: " + sshParameters.port;
			s += "\n\t\t<li>timeout (s): " + sshParameters.timeoutS;
			s += "\n\t</ul>\n";
		}

		s += "\n</ul>\n";
		return s.toString();
	}

	@Override
	public int hashCode() {
		return friendlyName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ComponentInfo && obj.hashCode() == hashCode();
	}

	public List<TransportLayer> getUseableProtocols() {
		List<TransportLayer> r = new ArrayList<>();
		UDPDriver udp = new UDPDriver();

		if (udp.canContact(this)) {
			r.add(udp);
		}

		return r;
	}

	public static List<ComponentInfo> fromPDL(List<String> parms) throws Throwable {
		List<ComponentInfo> r = new ArrayList<>();

		for (String s : parms) {
			r.add(fromCDL(s));
		}

		return r;
	}

	public static void main(String[] args) throws Throwable {
		ComponentInfo p = ComponentInfo.fromCDL(args[0]);
		System.out.println(p.toTDL());
	}

	public boolean isLocalhost() {
		for (InetAddress ip : inetAddresses) {
			if (!NetUtilities.isLocalhost(ip)) {
				return false;
			}
		}

		return true;
	}
}
