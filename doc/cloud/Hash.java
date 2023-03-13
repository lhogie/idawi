package idawi.service.cloud;

import java.util.Arrays;
import java.util.List;

public class Hash {
	long value;
	long length; // use the length as secondary hashing method

	@Override
	public boolean equals(Object obj) {
		var h = (Hash) obj;
		return value == h.value && length == h.length;
	}

	@Override
	public String toString() {
		return "value: " + value + ", len: " + length;
	}

	public static Hash merge(List<Hash> l) {
		var r = new Hash();
		var a = new long[l.size()];

		for (int i = 0; i < a.length; ++i) {
			var thisHash = l.get(i);
			a[i] = thisHash.value;
			r.length += thisHash.length;
		}

		r.value = Arrays.hashCode(a);
		return r;
	}
}
