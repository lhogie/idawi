package idawi.service.serialTest;

import java.net.DatagramPacket;
import java.util.Collection;

import com.fazecast.jSerialComm.*;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class serialTest extends TransportService {

    public serialTest(Component c) {
        super(c);
    }

    public byte[] startSend(String data) {
        Message msg = new Message<>();
        msg.content = data;
        byte[] msgBytes = serializer.toBytes(msg);

        return msgBytes;
    }

    public static void main(String[] args) {
        String dataToSend = "Hello";
        Component c = new Component();
        serialTest serialObject = new serialTest(c);
        byte[] msg = serialObject.startSend(dataToSend);

        SerialPort comPort = SerialPort.getCommPort("COM7");

        comPort.setBaudRate(57600);
        comPort.setNumDataBits(8);
        comPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        comPort.setParity(SerialPort.NO_PARITY);

        comPort.openPort();

        try {
            while (true) {
                Thread.sleep(1000);
                int bytesWritten = comPort.writeBytes(msg, msg.length);

                System.out.println(msg + " " + bytesWritten);
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
