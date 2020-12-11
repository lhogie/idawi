package idawi.ui.gui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import idawi.Component;
import idawi.ui.gui.UserFeedback.Entry.TYPE;
import toools.exceptions.ExceptionUtilities;
import toools.gui.Swingable;

public class UserFeedback_swing extends UserFeedback implements Swingable {
	private final JList<Entry> list = new JList<>();
	private final JTextArea label = new JTextArea();
	private final JPanel panel = new JPanel(new BorderLayout());

	public UserFeedback_swing(Component peer) {
		super(peer);
		label.setBackground(Color.white);
		panel.add(BorderLayout.CENTER, new JScrollPane(list));
		panel.add(BorderLayout.SOUTH, label);

		list.addListSelectionListener(e -> {
			Entry t = list.getSelectedValue();
			show(t);
		});
	}
	@Override
	public String getFriendlyName() {
		return "shows feedback in Swing";
	}
	@Override
	public JComponent getComponent() {
		return panel;
	}

	@Override
	protected void newFeedback(Entry e) {
		show(e);
	}

	private void show(Entry e) {
		if (e.type == TYPE.error) {
			label.setForeground(Color.red);
			label.setText("Error: " + e.value);
		}
		else if (e.type == TYPE.exception) {
			label.setForeground(Color.red);
			label.setText(ExceptionUtilities.toString((Throwable) e.value));
		}
		else if (e.type == TYPE.warning) {
			label.setForeground(Color.blue);
			label.setText("warning: " + e.value);
		}
		else if (e.type == TYPE.msg) {
			label.setForeground(Color.black);
			label.setText(e.value.toString());
		}
		else {
			throw new IllegalStateException();
		}
	}
}
