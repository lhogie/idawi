package idawi.ui.gui;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.NeighborhoodListener;
import idawi.TransportLayer;
import idawi.net.MultiTransport;
import idawi.net.NetworkingService;
import idawi.net.UDPDriver;
import idawi.ui.JThingLineCmd;
import j4u.CommandLine;
import toools.gui.Swingable;
import toools.io.file.RegularFile;

public class start extends JThingLineCmd {
	public start(RegularFile launcher) {
		super(launcher);
	}

	public static void main(String[] args) throws IOException {
	}

	@Override
	public int runScript(CommandLine cmdLine) throws Throwable {
		List<String> names = cmdLine.findParameters();

		if (names.isEmpty()) {
			names.add(System.getProperty("user.name") + "@"
					+ InetAddress.getLocalHost().getHostName());
		}

		int nbPeers = names.size();
		int nbCols = (int) Math.sqrt(nbPeers);

		if (nbCols * nbCols < nbPeers)
			++nbCols;

		int nbRows = (int) Math.sqrt(nbPeers);
		Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getMaximumWindowBounds();
		int width = Math.min(screenSize.width / nbCols, 600);
		int height = Math.min(screenSize.height / nbRows, 800);
		Set<Component> peers = new HashSet<>();

		for (int i = 0; i < nbPeers; ++i) {
			String name = names.get(i);
			MultiTransport mp = new MultiTransport();
			UDPDriver udp = new UDPDriver();
			mp.addProtocol(udp);

			Component peer = new Component(ComponentInfo.fromCDL("name="+name));

			peers.add(peer);

			JFrame f = new JFrame("P2P - " + name);

			peer.lookupService(NetworkingService.class).transport.listeners.add(new NeighborhoodListener() {
				@Override
				public void peerLeft(ComponentInfo p, TransportLayer protocol) {
					upateTitle();
				}

				@Override
				public void peerJoined(ComponentInfo newPeer, TransportLayer protocol) {
					upateTitle();
				}

				private void upateTitle() {
					f.setTitle(start.class.getPackage().getName() + " - " + name + " - ("
							+ peer.lookupService(NetworkingService.class).neighbors().size() + " neighbor(s))");
				}
			});

			JTabbedPane tabs = new JTabbedPane();
			f.setContentPane(tabs);
			f.setSize(width, height);
			f.setLocation(width * (i % nbCols), height * (i / nbCols));

			// new BeaconingService(peer);
			new Neighbors(peer);
			new SystemMonitor(peer);
			// new BroadcastService(peer);
			// new ErrorLog(peer);
			// AddressBookApp(peer);
			// Swingable chat = new Chat2(peer);
			// new MapApplication(peer);
			// new ExitApplication(peer);
			// new ExecApplication(peer);

			peer.services().stream().filter(app -> app instanceof Swingable)
					.forEach(app -> tabs.addTab(app.id.toString(),
							((Swingable) app).getComponent()));

			// tabs.setSelectedComponent(chat.getComponent());
			f.setVisible(true);
		}

		for (Component p : peers) {
			for (Component p2 : peers) {
				if (p != p2) {
					p.descriptorRegistry.add(p2.descriptor());
				}
			}
		}

		return 0;
	}

	@Override
	public String getShortDescription() {
		return "boh";
	}
}
