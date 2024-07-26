package toools.net.rs232;

import java.util.List;

public interface Lib<P extends SerialPort> {
	List<P> listPorts();
}
