package idawi.transport.serial;

import java.io.Serializable;

public class Param implements Serializable {
	public final String code;
	public final String name;
	public  int value;

	public Param(String code, String name, int value) {
		this.code = code;
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return name + "=" + value;
	}
}
