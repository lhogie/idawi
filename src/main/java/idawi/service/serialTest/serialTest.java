package idawi.service.serialTest;

import java.net.DatagramPacket;
import java.util.Collection;
import java.net.DatagramSocket;

import com.fazecast.jSerialComm.*;
import java.io.IOException;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class serialTest extends TransportService {
    private DatagramSocket socket;

    public serialTest(Component c) {
        super(c);
    }

    public void startSerial(byte[] data, int length) {

        DatagramPacket p = new DatagramPacket(data, length);

        try {
            // Cout.info("reading packet");
            socket.receive(p);
            Message msg = (Message) serializer.fromBytes(p.getData());
            // Cout.info("UDP received " + msg);
            // Cout.debugSuperVisible(msg.ID);
            processIncomingMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

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

    @Override
    public String getName() {
        return "loopback";
    }

    @Override
    public void dispose(Link l) {
        // l.activity.close();
    }

    @Override
    public double latency() {
        return 0;
    }

    @Override
    protected void multicast(byte[] msg, Collection<Link> outLinks) {
        var msgClone = (Message) serializer.fromBytes(msg);
        Idawi.agenda.scheduleNow(() -> processIncomingMessage(msgClone));
    }

    @Override
    protected void bcast(byte[] msg) {
        var msgClone = (Message) serializer.fromBytes(msg);
        Idawi.agenda.scheduleNow(() -> processIncomingMessage(msgClone));
    }

}
