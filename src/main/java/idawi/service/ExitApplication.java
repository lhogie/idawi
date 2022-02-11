package idawi.service;

import java.awt.GridBagLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import idawi.Component;
import idawi.TypedOperation;
import idawi.QueueAddress;
import idawi.Service;
import idawi.To;
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

		registerOperation(new exit());
	}

	private final static int SHUTDOWN = 56;
	private final static int RESTART = 57;

	@Override
	public String getFriendlyName() {
		return "exit";
	}

	public class exit extends TypedOperation {
		public void f(int code) {
			trigger(code);
		}

		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public void trigger(int exitCode) {
		QueueAddress to = new To().s(id).q(null);
		send(exitCode, to);
		component.forEachService(s -> s.dispose());
		System.exit(exitCode);
	}

	@Override
	public JComponent getComponent() {
		return buttons;
	}
}
