package idawi.knowledge_base;

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
import java.util.function.Consumer;

import idawi.CDLException;
import idawi.service.LocationService2.Location;
import idawi.service.SystemMonitor.SystemInfo;
import idawi.service.time.TimeModel;
import toools.net.NetUtilities;
import toools.text.TextUtilities;
import toools.util.Date;

/**
 * Specifies how to reach a given peer.
 * 
 * @author lhogie
 *
 */
public class ComponentDescription extends Info {

	enum WHERE {
		here, new_jvm, ssh
	}

	// optional cache to the ref
	public ComponentRef name;
//	public SSHParms ssh = new SSHParms();
	public final List<InetAddress> inetAddresses = new ArrayList<>();
	public Integer tcpPort, udpPort;
//	public Set<Class<? extends Service>> services = new HashSet<>();
	public Set<ServiceDescriptor> services = new HashSet<>();
	// private int isLocalhost;
	public SystemInfo systemInfo;
	public Location location;
	public TimeModel timeModel;

	// final Map<String, Object> map = new HashMap<>();

	public ComponentDescription(double date) {
		super(date);
	}

	@Override
	public String toString() {
		return name.toString();
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

		StringWriter w = new StringWriter();

		try {
			props.store(w, "this is a peer description");
			return w.toString();
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	public interface CDLHandler {
		void f(ComponentDescription p, String value) throws CDLException;
	}

	static Map<String, CDLHandler> key2cdlHandler = new HashMap<>();

	static {
		key2cdlHandler.put("name", (p, v) -> p.name = new ComponentRef(v));
		key2cdlHandler.put("tcp_port", (p, v) -> p.tcpPort = Integer.valueOf(v));
		key2cdlHandler.put("udp_port", (p, v) -> p.udpPort = Integer.valueOf(v));
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

	public static ComponentDescription fromName(ComponentRef name) {
		ComponentDescription d = new ComponentDescription(Date.time());
		d.name = name;
		return d;
	}

	public static ComponentDescription fromCDL(String s) {
		ComponentDescription peer = new ComponentDescription(Date.time());

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
			try {
				peer.inetAddresses.add(InetAddress.getLocalHost());
			} catch (UnknownHostException e1) {
				// no network on that computer, not too important
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
		return obj instanceof ComponentDescription && obj.hashCode() == hashCode();
	}

	public static List<ComponentDescription> fromPDL(List<String> parms) throws Throwable {
		List<ComponentDescription> r = new ArrayList<>();

		for (String s : parms) {
			r.add(fromCDL(s));
		}

		return r;
	}

	public static void main(String[] args) throws Throwable {
		ComponentDescription p = ComponentDescription.fromCDL(args[0]);
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

	@Override
	public boolean involves(ComponentRef d) {
		return name.equals(d.ref);
	}

	@Override
	public void forEachComponent(Consumer<ComponentRef> c) {
		if (name != null) {
			c.accept(name);
		}
	}

	public void consider(ComponentDescription d) {
		// TODO Auto-generated method stub

	}

}
