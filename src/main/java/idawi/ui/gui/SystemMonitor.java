package idawi.ui.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import idawi.Component;
import idawi.Service;
import idawi.net.NetworkingService;
import toools.gui.Swingable;

public class SystemMonitor extends Service implements Swingable {
	private int nbMsgs;
	private final JLabel label = new JLabel();

	public SystemMonitor(Component peer) {
		super(peer);
		newThread_loop_periodic(1000, () -> update());
	}

	@Override
	public String getFriendlyName() {
		return "shown monitoring";
	}

	private synchronized void update() {
		String s = "<html>";
		s += "<br>" + nbMsgs + " messages";
		s += "<br>" + component.lookup(NetworkingService.class).neighbors().size() + " peers";
		s += "<ul>";

		for (Object n : component.lookup(NetworkingService.class).neighbors()) {
			s += "<li>" + n;
		}

		s += "</ul><br>" + component.services().size() + " applications";
		s += "<ul>";

		for (Service n : component.services()) {
			s += "<li>" + n.id;
		}

		final String b = s;
		try {
			SwingUtilities.invokeAndWait(() -> label.setText(b));
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public JComponent getComponent() {
		return label;
	}

	@Override
	public void dispose() {
	}
}
