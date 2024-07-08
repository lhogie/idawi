package idawi.service.serialTest;

import com.fazecast.jSerialComm.*;

public class serialTest {

    public static void main(String[] args) {
        String dataToSend = "Hello, Serial Port!";
        byte[] data = dataToSend.getBytes();
        SerialPort comPort = SerialPort.getCommPort("COM7");
        comPort.setBaudRate(57600);
        comPort.setNumDataBits(8);
        comPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        comPort.setParity(SerialPort.NO_PARITY);

        comPort.openPort();

        try {
            while (true) {
                Thread.sleep(1000);
                int bytesWritten = comPort.writeBytes(data, data.length);
                System.out.println(bytesWritten);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        comPort.closePort();
    }
}
