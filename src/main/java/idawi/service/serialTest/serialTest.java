package idawi.service.serialTest;

import com.fazecast.jSerialComm.*;

public class serialTest {

    public static void main(String[] args) {
        String dataToSend = "Hello, Serial Port!";
        byte[] data = dataToSend.getBytes();
        SerialPort comPort = SerialPort.getCommPort("COM7");
        comPort.openPort();
        try {
            while (true) {
                Thread.sleep(1000);
                int bytesWritten = comPort.writeBytes(data, data.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        comPort.closePort();
    }
}
