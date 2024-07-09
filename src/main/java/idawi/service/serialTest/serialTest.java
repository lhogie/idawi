package idawi.service.serialTest;

import java.net.DatagramPacket;

import com.fazecast.jSerialComm.*;

import idawi.messaging.Message;

public class serialTest {

    public static void main(String[] args) {
        String dataToSend = "Hello, Serial Port!";
        byte[] data = dataToSend.getBytes();
        SerialPort comPort = SerialPort.getCommPort("COM7");
        comPort.setBaudRate(57600);
        comPort.setNumDataBits(8);
        comPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        comPort.setParity(SerialPort.NO_PARITY);
        DatagramPacket p = new DatagramPacket(data, data.length);

                            try {
                                // Cout.info("reading packet");
                                socket.receive(p);
                                Message msg = (Message) serializer.fromBytes(p.getData());
                                // Cout.info("UDP received " + msg);
                                // Cout.debugSuperVisible(msg.ID);
                                processIncomingMessage(msg);}
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
