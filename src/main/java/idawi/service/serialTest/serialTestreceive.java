package idawi.service.serialTest;

import java.io.InputStream;

import com.fazecast.jSerialComm.*;

public class serialTestreceive {

    public static void main(String[] args) {
        SerialPort comPort = SerialPort.getCommPort("/dev/ttyUSB0");
        comPort.openPort();
        InputStream numRead = comPort.getInputStream();

        try {
            while (true)
            {
               while (comPort.bytesAvailable() == 0)
                  Thread.sleep(1000);
         
               System.out.print((char)numRead.read());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        comPort.closePort();
    }
}
