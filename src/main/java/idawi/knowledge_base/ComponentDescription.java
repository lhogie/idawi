package idawi.knowledge_base;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import idawi.Component;
import idawi.service.LocationService.Location;
import idawi.service.SystemMonitor.SystemInfo;
import idawi.service.time.TimeModel;
import toools.net.NetUtilities;
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
	public ComponentRef ref;
	public boolean suicideWhenParentDie = true;

	public Class<? extends Component> componentClass = Component.class;
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
		return ref.toString();
	}

	public static ComponentDescription fromName(ComponentRef name) {
		ComponentDescription d = new ComponentDescription(Date.time());
		d.ref = name;
		return d;
	}

	@Override
	public int hashCode() {
		return ref.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ComponentDescription && obj.hashCode() == hashCode();
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
		return ref.equals(d.ref);
	}

	@Override
	public void forEachComponent(Consumer<ComponentRef> c) {
		if (ref != null) {
			c.accept(ref);
		}
	}

	public void consider(ComponentDescription d) {
		// TODO Auto-generated method stub

	}

}
