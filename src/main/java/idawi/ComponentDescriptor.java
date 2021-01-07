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

import idawi.net.TransportLayer;
import idawi.net.UDPDriver;
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
public class ComponentDescriptor implements Descriptor {
	enum WHERE {
		here, new_jvm, ssh
	}

	public long id = ThreadLocalRandom.current().nextLong();
	public String friendlyName;
	public SSHParms sshParameters = new SSHParms();
	public final List<InetAddress> inetAddresses = new ArrayList<>();
	public Integer tcpPort, udpPort;
	public final List<String> webServers = new ArrayList<>();
	public WHERE where;
//	public Set<Class<? extends Service>> services = new HashSet<>();
	public Set<ServiceDescriptor> services = new HashSet<>();
	// private int isLocalhost;

	// final Map<String, Object> map = new HashMap<>();

	public Set<String> neighbors = new HashSet<>();

	@Override
	public String toString() {
		String s = friendlyName.toString();
		return s;
	}

	public String toCDL() {
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

	public interface CDLHandler {
		void f(ComponentDescriptor p, String value) throws CDLException;
	}

	static Map<String, CDLHandler> key2cdlHandler = new HashMap<>();

	static {
		key2cdlHandler.put("name", (p, v) -> p.friendlyName = v);
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

	@Override
	public int hashCode() {
		return friendlyName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ComponentDescriptor && obj.hashCode() == hashCode();
	}

	public List<TransportLayer> getUseableProtocols() {
		List<TransportLayer> r = new ArrayList<>();
		UDPDriver udp = new UDPDriver();

		if (udp.canContact(this)) {
			r.add(udp);
		}

		return r;
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
