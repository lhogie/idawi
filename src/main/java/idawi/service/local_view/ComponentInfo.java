package idawi.service.local_view;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import idawi.Component;
import idawi.service.Location;
import idawi.service.SystemMonitor.SystemInfo;
import idawi.service.time.TimeModel;
import toools.net.NetUtilities;

/**
 * Specifies how to reach a given peer.
 * 
 * @author lhogie
 *
 */
public class ComponentInfo extends Info {

	public Component component;
	public boolean suicideWhenParentDie = true;

//	public SSHParms ssh = new SSHParms();
	public final List<InetAddress> inetAddresses = new ArrayList<>();
	public Integer tcpPort, udpPort; // no primitive so it can be null
//	public Set<Class<? extends Service>> services = new HashSet<>();
	public Set<ServiceInfo> services = new HashSet<>();
	// private int isLocalhost;
	public SystemInfo systemInfo;
	public Location location;
	public TimeModel timeModel;

	// final Map<String, Object> map = new HashMap<>();

	public ComponentInfo(double date) {
		super(date);
	}

	@Override
	public String toString() {
		return component + " info";
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
	public void exposeComponent(Predicate<Component> p) {
		p.test(component);
	}
}
