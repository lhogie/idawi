package idawi.ui.cmd;

import java.text.DecimalFormat;
import java.util.function.Consumer;

import idawi.Component;
import idawi.service.Bencher;

public class benchBackend extends CommandBackend {

	@Override
	public void runOnServer(Component thing, Consumer<Object> out)
			throws Throwable {
		int size = Integer.valueOf(cmdline.getOptionValue("--size"));
		
		
		
		Bencher.localBench(size, fdbk -> {
			if (fdbk instanceof Long) {
				DecimalFormat formatter = new DecimalFormat();
				out.accept(formatter.format((Long) fdbk) + "ns");
			}
		});
	}
}
