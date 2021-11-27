package idawi.service;

import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import idawi.Component;
import idawi.ComponentAddress;
import idawi.QueueAddress;
import idawi.Service;
import toools.gui.Swingable;

public class ExitApplication extends Service implements Swingable {
	JPanel buttons = new JPanel(new GridBagLayout());

	public ExitApplication(Component peer) {
		super(peer);

		JButton shutdownButton = new JButton("Shutdown");
		shutdownButton.addActionListener(e -> trigger(SHUTDOWN));

		JButton restartButton = new JButton("Restart");
		restartButton.addActionListener(e -> trigger(RESTART));

		buttons.add(shutdownButton);
		buttons.add(restartButton);

		registerOperation(null, in -> trigger((int) in.get_blocking().content));
	}

	private final static int SHUTDOWN = 56;
	private final static int RESTART = 57;

	@Override
	public String getFriendlyName() {
		return "exit";
	}

	public void trigger(int exitCode) {
		QueueAddress to = new ComponentAddress().s(id).q(null);
		send(exitCode, to);

		for (Service app : component.services()) {
			app.shutdown();
		}

		System.exit(exitCode);
	}

	@Override
	public JComponent getComponent() {
		return buttons;
	}
}
