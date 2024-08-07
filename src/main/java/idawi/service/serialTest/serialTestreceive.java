package idawi.service.serialTest;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import com.fazecast.jSerialComm.*;

import idawi.Component;
import idawi.Idawi;
import idawi.bachir.App;
import idawi.messaging.Message;
import idawi.transport.Link;
import idawi.transport.TransportService;

public class serialTestreceive extends TransportService implements SerialPortMessageListener {
    public serialTestreceive(Component c) {
        super(c);
    }

    public static final long serialVersionUID = 6207309244483020844L;
    public static int countlines = 0;
    public static int truelines = 0;
    public static float finallines = 0;
    public static byte[] bufferData;
    public static byte[] tempbufferData;
    public static int advanceBuffer = 0;

    public static void main(String[] args) {
        Idawi.agenda.start();

        Component c = new Component();
        serialTestreceive testos = new serialTestreceive(c);
        new App.S(c);
        SerialPort comPort = SerialPort.getCommPort("/dev/ttyUSB0");

        comPort.setBaudRate(57600);
        comPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
        comPort.openPort();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        comPort.addDataListener(testos);
        try {
            while (true) {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        comPort.removeDataListener();
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
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] delimitedMessage = event.getReceivedData();
        String t = new String(delimitedMessage);
        byte[] lenghtbytes = new byte[4];
        ByteBuffer byteBufferReader = ByteBuffer.wrap(delimitedMessage);
        // System.out.println("data : "+t);
        try {
            truelines = truelines + 1;
            if (delimitedMessage.length >= 28) {
                byteBufferReader.position(24);
                byteBufferReader.get(lenghtbytes, 0, 4);
                int lengthValue = ByteBuffer.wrap(lenghtbytes).getInt();
                System.out.println("True : " + delimitedMessage.length);
                System.out.println("lengthValue : " + lengthValue);
                if (delimitedMessage.length - 28 >= lengthValue) {
                    byte[] delimitedMessageHashcode = new byte[4];
                    byteBufferReader.position(delimitedMessage.length - 4);
                    byteBufferReader.get(delimitedMessageHashcode, 0, 4);
                    int receivedHashCode = ByteBuffer.wrap(delimitedMessageHashcode).getInt();
                    byte[] msgBytes = new byte[lengthValue];
                    byteBufferReader.position(28);
                    byteBufferReader.get(msgBytes, 0, lengthValue);
                    if (receivedHashCode == Arrays.hashCode(msgBytes)) {
                        System.out.println("HashCode Clear");
                        try {

                            Message msg = (Message) serializer.fromBytes(msgBytes);
                            processIncomingMessage(msg);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    tempbufferData = new byte[delimitedMessage.length - 28];// Buffer that will contain the message and
                                                                            // the checksum of the previous msg
                    byteBufferReader.position(28);
                    byteBufferReader.get(tempbufferData, 0, delimitedMessage.length - 28);
                    byte[] allByteArray = new byte[tempbufferData.length + delimitedMessage.length];
                    ByteBuffer buff = ByteBuffer.wrap(allByteArray);
                    buff.put(tempbufferData);
                    buff.put(delimitedMessage);
                    bufferData = buff.array();// Buffer with all previousData
                    //
                    String tcombined = new String(bufferData);
                    System.out.println(tcombined);
                    //
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] getMessageDelimiter() {
        byte[] marker = "fgmfkdjgvhdfkghksfjhfdsj".getBytes();
        return marker;
    }

    @Override
    public boolean delimiterIndicatesEndOfMessage() {

        return false;
    }

}
