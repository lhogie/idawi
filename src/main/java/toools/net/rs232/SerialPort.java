package toools.net.rs232;

import java.io.InputStream;
import java.io.OutputStream;

public interface SerialPort {
	String getID();

	InputStream inputStream();

	OutputStream outputStream();

	void setBaudRate(int b);

	int getBandRate();

	int setPower(int p);

	void getPower();
}
