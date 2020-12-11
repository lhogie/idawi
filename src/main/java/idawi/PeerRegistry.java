package idawi;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import toools.io.file.Directory;
import toools.io.file.RegularFile;

public class PeerRegistry extends HashSet<ComponentInfo> implements Serializable {
	public final static Directory d = new Directory(
			"$HOME/" + PeerRegistry.class.getPackage().getName() + "/addressBooks");

	public PeerRegistry() {
	}

	public PeerRegistry(Collection<ComponentInfo> c) {
		super(c);
	}

	@Override
	public boolean add(ComponentInfo p) {
		if (p == null) {
			throw new IllegalStateException();
		}

		return super.add(p);
	}

	public PeerRegistry(Directory directory) {

		if (!directory.exists()) {
			// Cout.warning("address book directory " + directory + " cannot be
			// found");
		}
	}

	public Set<ComponentInfo> lookup(Predicate<ComponentInfo> p) {
		return stream().filter(p).collect(Collectors.toSet());
	}

	public ComponentInfo lookupSingle(Predicate<ComponentInfo> p) {
		List<ComponentInfo> r = stream().filter(p).collect(Collectors.toList());

		if (r.size() == 1) {
			return r.get(0);
		}

		throw new IllegalStateException();
	}

	public ComponentInfo lookup(InetAddress ip) {
		return lookupSingle(p -> p.inetAddresses.contains(ip));
	}

	public Set<ComponentInfo> lookupByName(String id) {
		return lookup(p -> p.friendlyName.equals(id));
	}

	public void loadCards(Directory d) {
		for (RegularFile file : d.listRegularFiles()) {
			add((ComponentInfo) file.getContentAsJavaObject());
		}
	}

	public Set<String> friendlyNames() {
		Set<String> r = new HashSet<>(size());

		for (ComponentInfo c : this) {
			if (c.friendlyName != null) {
				r.add(c.friendlyName);
			}
		}

		return r;
	}

	public String toHTML() {
		StringBuilder s = new StringBuilder("<ul>\n");
		forEach(c -> s.append("\t<h1>Card</h1><li>" + c.toHTML() + "\n"));
		s.append("</ul>\n");
		return s.toString();
	}

	public void addLocalPeerByPort(int... ports) throws UnknownHostException {
		for (int port : ports) {
			ComponentInfo c = new ComponentInfo();
			c.friendlyName = "peer-" + port;
			c.inetAddresses.add(InetAddress.getLocalHost());
			c.tcpPort = c.udpPort = port;
			add(c);
		}
	}

	public static Set<InetAddress> sshKnownHosts() {
		RegularFile knownHostsFile = new RegularFile("$HOME/.ssh/known_hosts");
		Set<String> firstElementOfLines = new HashSet<>();

		for (String l : knownHostsFile.getLines()) {
			String e = l.split(" ")[0];
			int i = e.indexOf(",");

			if (i < 0) {
				firstElementOfLines.add(e);
			} else {
				firstElementOfLines.add(e.substring(0, i));
			}
		}

		Set<InetAddress> ips = new HashSet<>();

		for (String e : firstElementOfLines) {
			try {
				ips.add(InetAddress.getByName(e));
			} catch (UnknownHostException e1) {
			}
		}

		return ips;
	}

	public static Set<InetAddress> oarNodes() {
		String oarNodesFileName = System.getenv("OAR_NODEFILE");
		RegularFile f = new RegularFile(oarNodesFileName);

		if (f.exists()) {
			return nodeList(f);
		} else {
			return null;
		}
	}

	public static Set<InetAddress> nodeList(RegularFile f) {
		Set<InetAddress> ips = new HashSet<>();

		for (String l : f.getLines()) {
			try {
				ips.add(InetAddress.getByName(l));
			} catch (UnknownHostException e1) {
			}
		}

		return ips;
	}

	public static PeerRegistry from(Set<InetAddress> ips) {
		PeerRegistry ab = new PeerRegistry();

		for (InetAddress ip : ips) {
			ComponentInfo c = new ComponentInfo();
			c.inetAddresses.add(ip);
			ab.add(c);
		}

		return ab;
	}

	public void update(ComponentInfo p) {
		add(p);
	}

	public ComponentInfo pickRandomPeer() {
		List<ComponentInfo> l = toList();
		return l.get(ThreadLocalRandom.current().nextInt(l.size()));
	}

	public List<ComponentInfo> toList() {
		return new ArrayList<ComponentInfo>(this);
	}

	public Set<ComponentInfo> lookupByNames(List<String> names) {
		Set<ComponentInfo> r = new HashSet<>();

		for (String n : names) {
			Set<ComponentInfo> p = lookupByName(n);

			if (p.isEmpty()) {
				throw new IllegalArgumentException("no peer with name: " + n);
			}

			r.addAll(p);
		}
		return r;
	}
}
