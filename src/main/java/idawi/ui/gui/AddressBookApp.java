package idawi.ui.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import idawi.Component;
import idawi.ComponentInfo;
import idawi.Service;
import toools.gui.Swingable;

public class AddressBookApp extends Service implements Swingable
{
	private final JScrollPane sp;

	public AddressBookApp(Component peer)
	{
		super(peer);
		JLabel label = new JLabel();
		sp = new JScrollPane(label);

		newThread_loop_periodic(1000, () -> {
			String s = "<html>";

			for (ComponentInfo c : peer.descriptorRegistry.toList())
			{
				s += "<br>" + c.toHTML();
			}
			final String b = s;
			try
			{
				SwingUtilities.invokeAndWait(() -> label.setText(b));
			}
			catch (InvocationTargetException | InterruptedException e)
			{
				e.printStackTrace();
			}
		});
	}
	@Override
	public String getFriendlyName() {
		return "display peer book";
	}
	@Override
	public JComponent getComponent()
	{
		return sp;
	}
}
