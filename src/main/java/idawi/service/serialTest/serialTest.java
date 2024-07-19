package idawi.service.serialTest;

import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;

import com.fazecast.jSerialComm.*;

import idawi.Component;
import idawi.Idawi;
import idawi.messaging.Message;
import idawi.transport.Link;
import idawi.transport.TransportService;
import toools.Systeem;

import java.nio.ByteBuffer;

public class serialTest extends TransportService {
    public static final byte[] marker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();

    public serialTest(Component c) {
        super(c);
    }

    public byte[] startSend(String data) {
        Message msg = new Message<>();
        msg.content = data;
        byte[] msgBytes = serializer.toBytes(msg);

        // byte[] concatBytes = ArrayUtils.addAll(marker, msgBytes);
        // System.out.println(marker);
        // System.out.println(msgBytes);
        // System.out.println(concatBytes);

        // return concatBytes;
        return msgBytes;
    }

    public static void main(String[] args) {
        int i = 0;
        String dataToSend = "Helcxvcxvcxlo";
        String dataToSend2 = "Yafdvfdvfdkmp";

        Component c = new Component();
        serialTest serialObject = new serialTest(c);
        byte[] msg = serialObject.startSend(dataToSend);
        byte[] msg2 = serialObject.startSend(dataToSend2);

        SerialPort comPort = SerialPort.getCommPort("COM8");
        SerialPort comPort2 = SerialPort.getCommPort("COM7");

        comPort.setBaudRate(57600);
        comPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
        comPort.openPort();

        comPort2.setBaudRate(57600);
        comPort2.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
        comPort2.openPort();

        try {
            while (i < 12) {
                byte[] lengthmsg = ByteBuffer.allocate(4).putInt(msg.length).array();
                System.out.println(ByteBuffer.wrap(lengthmsg).getInt() + "   " + lengthmsg.length);
                int intHashCode = Arrays.hashCode(msg);
                byte[] hashCode = ByteBuffer.allocate(4).putInt(intHashCode).array();
                System.out.println(intHashCode);

                byte[] lengthmsg2 = ByteBuffer.allocate(4).putInt(msg2.length).array();
                System.out.println(ByteBuffer.wrap(lengthmsg2).getInt() + "   " + lengthmsg2.length);
                int intHashCode2 = Arrays.hashCode(msg2);
                byte[] hashCode2 = ByteBuffer.allocate(4).putInt(intHashCode2).array();
                System.out.println(intHashCode2);
                // int bytesWrittenMarker = comPort.writeBytes(marker, marker.length);
                // Thread.sleep(500);
                // int bytesWrittenLength = comPort.writeBytes(lengthmsg, lengthmsg.length);
                // Thread.sleep(500);
                // int bytesWritten = comPort.writeBytes(msg, msg.length);
                // Thread.sleep(500);
                byte[] allByteArray = new byte[marker.length + lengthmsg.length + msg.length + hashCode.length];
                ByteBuffer buff = ByteBuffer.wrap(allByteArray);
                buff.put(marker);
                buff.put(lengthmsg);
                buff.put(msg);
                buff.put(hashCode);
                byte[] combined = buff.array();
                int bytesWrittenCombined = comPort.writeBytes(combined, combined.length);
                // Thread.sleep(500);
                System.out.println(combined + " " + bytesWrittenCombined);

                byte[] allByteArray2 = new byte[marker.length + lengthmsg2.length + msg2.length + hashCode2.length];
                ByteBuffer buff2 = ByteBuffer.wrap(allByteArray2);
                buff2.put(marker);
                buff2.put(lengthmsg2);
                buff2.put(msg2);
                buff2.put(hashCode2);
                byte[] combined2 = buff2.array();
                int bytesWrittenCombined2 = comPort2.writeBytes(combined2, combined2.length);
                // Thread.sleep(500);
                System.out.println(combined2 + " " + bytesWrittenCombined2);

                // System.out.println(marker + " " + bytesWrittenMarker);
                // System.out.println(lengthmsg + " " + bytesWrittenLength);
                // System.out.println(msg + " " + bytesWritten);
                i++;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        comPort.closePort();
        comPort2.closePort();

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

}
