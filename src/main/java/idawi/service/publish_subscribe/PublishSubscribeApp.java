package idawi.service.publish_subscribe;

import java.awt.GridLayout;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.NeighborhoodListener;
import idawi.Service;
import idawi.TransportLayer;
import idawi.net.Neighborhood;
import idawi.net.NetworkingService;
import toools.gui.Swingable;

public class PublishSubscribeApp extends Service implements Swingable {
	private final JPanel c = new JPanel(new GridLayout(2, 1));

	public PublishSubscribeApp(Component peer, long timeoutMs) {
		super(peer);

		JPanel browser = new JPanel(new GridLayout(2, 1));
		JList nodeList = new JList<>();
		JList topicList = new JList<>();
		browser.add(nodeList);
		browser.add(topicList);

		JTextArea renderer = new JTextArea();

		c.add(browser);
		c.add(renderer);
		peer.lookupService(NetworkingService.class).transport.listeners.add(new NeighborhoodListener() {
			@Override
			public void peerLeft(ComponentInfo peer, TransportLayer protocol) {
				((DefaultListModel) nodeList.getModel()).removeElement(peer);
			}

			@Override
			public void peerJoined(ComponentInfo peer, TransportLayer protocol) {
				((DefaultListModel) nodeList.getModel()).addElement(peer);
			}
		});
	}
	@Override
	public String getFriendlyName() {
		return "boh";
	}
	@Override
	public JComponent getComponent() {
		return c;
	}

}
