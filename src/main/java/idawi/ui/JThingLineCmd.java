package idawi.ui;

import j4u.CommandLineApplication;
import j4u.License;
import toools.io.file.RegularFile;

public abstract class JThingLineCmd extends CommandLineApplication
{
	public JThingLineCmd(RegularFile launcher)
	{
		super(launcher);
	}

	@Override
	public String getApplicationName()
	{
		return "jThing";
	}

	@Override
	public String getAuthor()
	{
		return "Luc Hogie";
	}

	@Override
	public License getLicence()
	{
		return License.ApacheLicenseV2;
	}

	@Override
	public String getYear()
	{
		return "2019";
	}
}
