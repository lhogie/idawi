package idawi;

import java.io.Serializable;

public class EOT implements Serializable {
	// a field is required to as serialization to GSON produces something
	public final String type = "EOT";
	private EOT() {
		
	}
	
	@Override
	public String toString() {
		return "EOT";
	}
	
	public static final EOT instance = new EOT();
}