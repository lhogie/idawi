package idawi.transport;

import java.util.Arrays;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.io.ByteArrayOutputStream;

import idawi.Component;

public class SikDevice extends SerialDevice {
    private static String lineSeparator = System.getProperty("line.separator");
    public String infosParsed = "";
    public boolean getFirstConfig = true;
    Config config = new Config();
    OutputStream os = this.getOutputStream();

    public SikDevice(Component c, InputStream inputStream, OutputStream outputStream) {

        super(c, inputStream, outputStream);

    }

    public Config fetchInitialConfig() {
        String[] infosDividedlines = this.infosParsed.split("\\n");
        Config intialConfig = new Config();
        String code;
        String name;
        int value;
        for (String string : infosDividedlines) {
            String[] splitString = string.split(":|=");
            code = splitString[0].replaceAll("[^\\d.]", "");
            name = splitString[1];
            value = Integer.parseInt(splitString[2].trim());
            Param p = new Param(code, name, value);
            intialConfig.addParam(p);
        }
        System.out.println(intialConfig);
        return intialConfig;
    }

    @Override
    public String toString() {
        return "Config in Sik Device : " + config;
    }

    private void setupMode() {
        try {
            Thread.sleep(1100);
            os.write("+++".getBytes());
            Thread.sleep(1100);

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void setParameter(String param, int value) {
        try {
            os.write((param + "=" + value).getBytes());
            os.write(lineSeparator.getBytes());
            Thread.sleep(100);
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void showSetup() {
        try {
            setupMode();
            os.write(("ATI5").getBytes());
            os.write(lineSeparator.getBytes());
            Thread.sleep(100);
            os.write(("ATO").getBytes());
            os.write(lineSeparator.getBytes());
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean setConfig(Config c) {
        try {
            config.modifyConfig(c);
            setupMode();
            for (Param param : config.getParams()) {
                setParameter("ATS" + param.getCode(), param.getValue());
            }
            save();
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    void save() {
        try {
            os.write("AT&W".getBytes());
            os.write(lineSeparator.getBytes());
            Thread.sleep(100);
            os.write("ATZ".getBytes());
            os.write(lineSeparator.getBytes());
            Thread.sleep(100);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void inputStreamDecoder() {
        try {

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (true) {

                int i = getInputStream().read();
                bytes.write((byte) i);
                checkShow(bytes);
                if ((bytes.size() >= marker.length) && endsBy(marker, bytes)) {
                    System.out.println("nice");
                    callback(Arrays.copyOf(bytes.toByteArray(), bytes.size() - marker.length));
                    bytes.reset();

                }
            }

        } catch (IOException err) {
            System.err.println("I/O error reading stream");
        }
    }

    private void checkShow(ByteArrayOutputStream bytes) {
        String stringRead = new String(bytes.toByteArray());
        if (stringRead.contains("ATO")) {

            String infos = stringRead.split("ATI5")[1].replace("ATO", "");
            infosParsed = infos.trim();
            if (getFirstConfig) {
                config = fetchInitialConfig();
            }
            bytes.reset();
        }
    }
    // public void allSetup(int serialSpeed, int airSpeed, int id, int power, int
    // ecc, int mav, int oppesend,
    // int minFreq, int maxFreq, int numChannels, int dutyCycle, int LBT, int
    // manchester, int rtscts,
    // int maxWindow) {
    // try {
    // Thread.sleep(200);
    // } catch (InterruptedException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // Config cf=new Config();
    // for (Param param : config.getParams()) {
    // Param p=new Param(param.getCode(), param.getName(), )
    // cf.addParam(param);
    // }
    // setConfig(c);

    // }
}
