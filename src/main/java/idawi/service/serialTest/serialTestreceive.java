package idawi.service.serialTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.io.IOUtils;

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
    public static final long serialVersionUID=6207309244483020844L;
    public static int countlines=0;
    public static int truelines=0;
    public static float finallines=0;
    public static byte[] bufferData;
    public static String bufferReader=new String();
    public static int advanceBuffer=0;
    private static boolean word=false;

    public static void main(String[] args) {
        Component c =new Component();
        serialTestreceive testos= new serialTestreceive(c);
        new App.S(c);
        SerialPort comPort = SerialPort.getCommPort("/dev/ttyUSB0");

        comPort.setBaudRate(57600);
        comPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
        comPort.openPort();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        comPort.addDataListener(testos);
        try { while (true) {
            
        } } catch (Exception e) { e.printStackTrace(); }
        comPort.removeDataListener();
        // try {
        //     while (true)serialTestreceive
        //     {
        //        while (comPort.bytesAvailable() == 0){
        //           Thread.sleep(1000);}
        //         byte[] readBuffer = new byte[comPort.bytesAvailable()] ;
        //         int numRead = comPort.readBytes(readBuffer, readBuffer.length);
        //         countlines=countlines+1;
        //         finallines=truelines/countlines;
        //         System.out.println(countlines+" "+truelines);
        //         System.out.println("buffer " + readBuffer.length + " bytes.");  
        //         System.out.println("Read " + numRead + " bytes.");  
        //         /* int availableBytes=comPort.bytesAvailable();
        //         System.out.println(availableBytes);
        //         byte[] bytes=numRead.readNBytes(availableBytes); */
        //         testos.startSerial(readBuffer);


        //     }
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
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
    public long calculateCR32(byte [] message){
          Checksum crc32 = new CRC32();
            crc32.update(message, 0, message.length);
            return crc32.getValue();
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] delimitedMessage = event.getReceivedData();
        String t=new String(delimitedMessage);
        byte[] lenghtbytes=new byte[4];
        ByteBuffer byteBufferReader=ByteBuffer.wrap(delimitedMessage);
        // System.out.println("data : "+t);
        try {
                        truelines=truelines+1;
                        if (delimitedMessage.length>=28){
                        byteBufferReader.position(24);
                        byteBufferReader.get(lenghtbytes,0,4);
                        int lengthValue=ByteBuffer.wrap(lenghtbytes).getInt();
                        if(delimitedMessage.length-28>=lengthValue){
                        byte[] delimitedMessageHashcode=new byte[4];
                        byteBufferReader.position(delimitedMessage.length-4);
                        byteBufferReader.get(delimitedMessageHashcode,0,4);
                        int receivedHashCode=ByteBuffer.wrap(delimitedMessageHashcode).getInt();
                        byte[] msgBytes=new byte[lengthValue];
                        byteBufferReader.position(28);
                        byteBufferReader.get(msgBytes,0,lengthValue);
                        if(receivedHashCode==Arrays.hashCode(msgBytes)){
                            System.out.println("yeees");
                        try{
                         
                        Message msg= (Message)serializer.fromBytes(msgBytes);
                        System.out.println(msg);
                        processIncomingMessage(msg);

                    }
                        catch (Exception e) {
                            e.printStackTrace();
                            // TODO: handle exception
                        }}}
                    }
                                 

            

          
        } catch (Exception e) {
            e.printStackTrace();
                }        }

    @Override
    public byte[] getMessageDelimiter() {
        byte[] marker= "fgmfkdjgvhdfkghksfjhfdsj".getBytes();
return marker;   }

    @Override
    public boolean delimiterIndicatesEndOfMessage() {

       return false;
    }

}
