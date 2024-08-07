package idawi.transport;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import com.fazecast.jSerialComm.SerialPort;

import idawi.Component;
import idawi.Idawi;

public class SerialDriver {

    private static ArrayList<SerialPort> serialOpen = new ArrayList<>();
    private ArrayList<SerialPort> openedSerialPorts = new ArrayList<>();

    public SerialDriver(Component c) {
        Idawi.agenda.threadPool.submit(() -> {
            try {
                openPorts(c);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // TODO Auto-generated constructor stub
    }

    public void fetchSiKDevices(Component c) {
        ArrayList<SerialPort> serialPorts = serialDevices();
        serialPorts.removeAll(openedSerialPorts);
        openedSerialPorts = serialPorts;

        for (SerialPort serial : serialPorts) {

            SikDevice device = new SikDevice(c, serial.getInputStream(), serial.getOutputStream());
            threadAllocator(device);
            System.out.println("okay for fetch");

        }
    }

    public boolean checkOpenArray(SerialPort serialPort) {

        for (SerialPort serialPortOpen : serialOpen) {
            if (serialPortOpen.getDescriptivePortName().equalsIgnoreCase(serialPort.getDescriptivePortName())) {

                return true;

            }

        }
        return false;

    }

    public void openPorts(Component c) throws InterruptedException {
        boolean serialOpenContains = false;
        while (true) {
            SerialPort[] allPorts = SerialPort.getCommPorts();
            boolean addition = false;
            for (SerialPort serialPort : allPorts) {
                serialOpenContains = checkOpenArray(serialPort);
                if (!serialPort.isOpen() && !serialOpenContains) {
                    if ((!serialPort.getDescriptivePortName().contains("Bluetooth"))
                            && (!serialPort.getDescriptivePortName().contains("S4"))) {
                        serialPort.openPort();
                        serialPort.setBaudRate(115200);
                        serialPort.setFlowControl(
                                SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
                        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
                        serialOpen.add(serialPort);
                        addition = true;
                    }
                }
            }
            if (addition) {
                fetchSiKDevices(c);
                // threadAllocator();
            }
            Thread.sleep(1000);

        }
    }

    private void newThread(SikDevice device) {
        Idawi.agenda.threadPool.submit(() -> {
            try {
                device.inputStreamDecoder();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void threadAllocator(SikDevice device) {
        newThread(device);
        device.showSetup();

    }

    protected ArrayList<SerialPort> serialDevices() {
        return serialOpen;
    }

}
