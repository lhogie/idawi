package idawi.ui.cmd;

import java.util.function.Consumer;

import idawi.Component;
import idawi.Service;

public class lsBackend extends CommandBackend {

	@Override
	public void runOnServer(Component thing, Consumer<Object> out) throws Throwable {
		boolean lsQueues = cmdline.isOptionSpecified("-q");

		for (Service s : thing.services()) {
			out.accept(s.getFriendlyName() + " (" + s.id + ")");

			if (lsQueues) {
				for (var od : s.descriptor().endpoints) {
					out.accept("\t- " + od.name);
				}
			}
		}
	}
}
