package idawi;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2CharMap;
import it.unimi.dsi.fastutil.objects.Object2CharOpenHashMap;

public class InputDescription {
	public static String shortcuts = "abcdefghijkmlnopqrstuvwxyz";

	public String re;
	public final Char2ObjectMap<Class> shortcut_classname = new Char2ObjectOpenHashMap<>();
	public final Object2CharMap<Class> classname_shortcut = new Object2CharOpenHashMap<>();

	public char shortcutFor(Class c) {
		if (classname_shortcut.containsKey(c)) {
			return classname_shortcut.getChar(c); 
		}else {
			char s = shortcuts.charAt(shortcut_classname.size());
			shortcut_classname.put(s, c);
			return s;
		}
	}
}
