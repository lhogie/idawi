package idawi.transport.serial;

import java.util.ArrayList;
import java.util.function.Predicate;

public class Config extends ArrayList<Param> {
	/**
	 * Parsing des données lues et conversion en objet Config
	 */
	public static Config from(String text) {

		var splitedText = text.substring(text.indexOf("S0")).replace("ATO", "").split("\\r?\\n");
		var c = new Config();
		for (String line : splitedText) {
			String[] a = line.split(":|=");
			Integer.valueOf(a[2].trim());
			c.add(new Param(a[0], a[1], Integer.valueOf(a[2].trim())));
		}
		return c;
	}

	@Override
	public String toString() {
		return stream().map(p -> p.toString()).reduce((a, b) -> a + "\n" + b).get();
	}

	public Param find(Predicate<Param> predicate) {
		return stream().filter(predicate).findAny().get();
	}

	public Param findByName(String name) {
		return find(p -> p.name.equals(name));
	}
}
