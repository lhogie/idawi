package idawi;

import java.io.IOException;
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

import idawi.service.SystemMonitor.Info;
import toools.net.NetUtilities;
import toools.net.SSHParms;
import toools.text.TextUtilities;

/**
 * Specifies how to reach a given peer.
 * 
 * @author lhogie
 *
 */
public class ComponentDescriptor implements Descriptor {
	enum WHERE {
		here, new_jvm, ssh
	}

	public String name;
	public SSHParms sshParameters = new SSHParms();
	public final List<InetAddress> inetAddresses = new ArrayList<>();
	public Integer tcpPort, udpPort;
	public final List<String> webServers = new ArrayList<>();
	public WHERE where;
//	public Set<Class<? extends Service>> services = new HashSet<>();
	public Set<String> servicesNames = new HashSet<>();
	// private int isLocalhost;
	public Info systemInfo;

	// final Map<String, Object> map = new HashMap<>();

	public Map<String, Set<String>> protocol2neighbors = new HashMap<>();
	public Set<String> neighbors = new HashSet<>();
	public Map<String, Set<String>> neighbors2 = new HashMap<>();

	@Override
	public String toString() {
		String s = name.toString();
		return s;
	}

	public String toCDL() {
		Properties props = new Properties();
		props.put("name", name);
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

	public interface CDLHandler {
		void f(ComponentDescriptor p, String value) throws CDLException;
	}

	static Map<String, CDLHandler> key2cdlHandler = new HashMap<>();

	static {
		key2cdlHandler.put("name", (p, v) -> p.name = v);
		key2cdlHandler.put("tcp_port", (p, v) -> p.tcpPort = Integer.valueOf(v));
		key2cdlHandler.put("udp_port", (p, v) -> p.udpPort = Integer.valueOf(v));
		key2cdlHandler.put("ssh", (p, v) -> p.sshParameters = SSHParms.fromSSHString(v));
		key2cdlHandler.put("where", (p, v) -> p.where = WHERE.valueOf(v));
		key2cdlHandler.put("ip", (p, v) -> {
			for (String i : v.split(",")) {
				try {
					p.inetAddresses.add(InetAddress.getByName(i));
				} catch (UnknownHostException e) {
					new CDLException(e);
				}
			}
		});
	}

	public static ComponentDescriptor fromCDL(String s) {
		ComponentDescriptor peer = new ComponentDescriptor();

		s = s.replaceAll(" +", " ");
		s = s.replaceAll(" */ *", "\n");
		Properties props = new Properties();

		try {
			props.load(new StringReader(s));
		} catch (IOException e1) {
			throw new CDLException(e1);
		}

		for (Entry<Object, Object> e : props.entrySet()) {
			CDLHandler h = key2cdlHandler.get(e.getKey());

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

		if (peer.name == null) {
			if (!peer.inetAddresses.isEmpty()) {
				peer.name = peer.inetAddresses.get(0).getHostName();
			} else if (peer.sshParameters.hostname != null) {
				peer.name = peer.sshParameters.hostname;
			} else {
				peer.name = "thing-" + ThreadLocalRandom.current().nextInt();
			}
		}

		return peer;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ComponentDescriptor && obj.hashCode() == hashCode();
	}

	public static List<ComponentDescriptor> fromPDL(List<String> parms) throws Throwable {
		List<ComponentDescriptor> r = new ArrayList<>();

		for (String s : parms) {
			r.add(fromCDL(s));
		}

		return r;
	}

	public static void main(String[] args) throws Throwable {
		ComponentDescriptor p = ComponentDescriptor.fromCDL(args[0]);
		System.out.println(p.toCDL());
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
