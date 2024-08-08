package idawi.transport.serial;

import java.util.ArrayList;

public class Config extends ArrayList<Param> {

	public static Config from(String text) {
		var c = new Config();
		
		for (var line : text.split("\\n")) {
			String[] a = line.split(":|=");
			c.add(new Param(a[0], a[1], Integer.valueOf(a[2].trim())));
		}
		
		return c;
	}

	@Override
	public String toString() {
		return stream().map(p -> p.toString()).reduce((a, b) -> a + "\n" + b).get();
	}
}
