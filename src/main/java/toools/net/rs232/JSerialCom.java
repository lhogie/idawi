package toools.net.rs232;

import java.util.Arrays;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

public class JSerialCom implements Lib<JSerialCommPort> {

	@Override
	public List<JSerialCommPort> listPorts() {
		return Arrays.stream(SerialPort.getCommPorts()).map(p -> new JSerialCommPort()).toList();
	}
}
