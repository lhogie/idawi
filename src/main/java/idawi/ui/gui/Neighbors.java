package idawi.ui.gui;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import idawi.Component;
import idawi.Service;
import idawi.net.NetworkingService;
import toools.gui.Swingable;

public class Neighbors extends Service implements Swingable
{
	private final JLabel label = new JLabel();

	public Neighbors(Component peer)
	{
		super(peer);
		newThread_loop_periodic(1000, () -> update());
	}
	@Override
	public String getFriendlyName() {
		return "shown neighbors";
	}
	private synchronized void update()
	{
		StringBuilder text = new StringBuilder("<html>");
		text.append(component.lookupService(NetworkingService.class).alreadyReceivedMsgs.size() + " message(s) received");
		text.append(
				"<br>" + component.lookupService(NetworkingService.class).neighbors().size() + " neighbor(s)");
		text.append("<br><ul>");
		component.lookupService(NetworkingService.class).neighbors().forEach(n -> text.append("<li>" + n));

		try
		{
			SwingUtilities.invokeAndWait(() -> label.setText(text.toString()));
		}
		catch (InvocationTargetException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public JComponent getComponent()
	{
		return label;
	}
}
