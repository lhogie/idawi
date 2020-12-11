package idawi.lang;

import java.util.Properties;

public class ElementParms
{
	final private Properties properties = new Properties();

	public void addFlag(String flagname)
	{
		properties.put(flagname, true);
	}

	public void addAffectation(String varname, String value)
	{
		properties.put(varname, value);
	}

	public String remove(String name, Object defaultValue)
	{
		if (contains(name))
			return (String) properties.remove(name);

		return defaultValue.toString();
	}

	public String remove(String name)
	{
		if ( ! contains(name))
			throw new IllegalArgumentException("parameter not specified: " + name);

		return (String) properties.remove(name);
	}

	public boolean contains(String name)
	{
		return properties.containsKey(name);
	}

	public boolean containsAndRemove(String name)
	{
		return properties.remove(name) != null;
	}

	public void ensureAllUsed()
	{
		if ( ! properties.isEmpty())
			throw new IllegalStateException("unsupported parm: " + properties);
	}

	@Override
	public String toString()
	{
		return properties.toString();
	}

	public long getLong(String name, long defaultValue)
	{
		return contains(name) ? Long.valueOf(remove(name)) : defaultValue;
	}

	public void addStatement(String statement)
	{
		int i = statement.indexOf('=');

		// no affectation
		if (i == - 1)
		{
			addFlag(statement);
		}
		else
		{
			addAffectation(statement.substring(0, i), statement.substring(i + 1).trim());
		}
	}

}
