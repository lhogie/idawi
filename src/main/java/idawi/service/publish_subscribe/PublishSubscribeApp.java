package idawi.service.publish_subscribe;

import java.awt.GridLayout;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import idawi.Component;
import idawi.Service;
import idawi.knowledge_base.ComponentRef;
import idawi.knowledge_base.MapService;
import idawi.knowledge_base.NetworkTopologyListener;
import idawi.knowledge_base.info.DirectedLink;
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
		peer.lookup(MapService.class).map.listeners.add(new NetworkTopologyListener() {

			@Override
			public void newComponent(ComponentRef p) {
				((DefaultListModel) nodeList.getModel()).removeElement(peer);
			}

			@Override
			public void interactionStopped(DirectedLink l) {
				((DefaultListModel) nodeList.getModel()).removeElement(peer);
			}

			@Override
			public void newInteraction(DirectedLink l) {
				((DefaultListModel) nodeList.getModel()).removeElement(peer);
			}

			@Override
			public void componentHasGone(ComponentRef a) {
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
