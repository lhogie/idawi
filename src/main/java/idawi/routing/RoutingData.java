package idawi.routing;

import java.io.Serializable;
import java.util.Arrays;

import toools.SizeOf;
import toools.text.TextUtilities;

public abstract class RoutingData implements Serializable, SizeOf {
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return TextUtilities.concat(",", Arrays.stream(getClass().getDeclaredFields()).map(f -> {
			try {
				return f.getName() + ":" + f.get(this);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}).toList());
	}

	public abstract void fromString(String s, RoutingService service);

}
