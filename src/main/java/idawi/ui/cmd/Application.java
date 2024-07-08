package idawi.ui.cmd;

import j4u.License;

public abstract class Application extends j4u.Application {
	@Override
	public final String getAuthor() {
		return "Luc Hogie (CNRS)";
	}

	@Override
	public final License getLicence() {
		return License.ApacheV2;
	}

	@Override
	public final String getYear() {
		return "2020-2023";
	}

	@Override
	public String getVersion() {
		return null;
	}

	@Override
	public String getName() {
		return "Idawi";
	}

}
