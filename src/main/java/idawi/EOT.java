package idawi;

import java.io.Serializable;

public class EOT implements Serializable {
	private EOT() {
		
	}
	
	@Override
	public String toString() {
		return "EOT";
	}
	
	public static final EOT instance = new EOT();
}