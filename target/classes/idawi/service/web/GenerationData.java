package idawi.service.web;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TreeMap;

public class GenerationData {

    private TreeMap<Date, Integer> datas = new TreeMap<Date, Integer>();

    public GenerationData () {
        for (int i=15000; i>1000; i=i-1000) {
            this.generateNewValues(i);
        }
    }

    private String parseDateToJson (Date date) {
        return "[\"" + new SimpleDateFormat("yyyy-mm-dd").format(date) + "T" +
                new SimpleDateFormat("hh:mm:ss.SSS").format(date) + "Z\"," + datas.get(date) + "]";
    }

    public String getAllData () {
        String myjson = "[";
        int acc = this.datas.keySet().size();
        for (Date date : this.datas.keySet()) {
            myjson += this.parseDateToJson(date);
            if (acc > 1) { myjson += ","; }
            acc--;
        };
        return myjson + "]";
    }

    public String generateNewValues (int dec) {
        int min = 0, max = 100;
        Date key = new Date(System.currentTimeMillis() - dec);
        datas.put(key, new Random().nextInt((max - min) + 1) + min);

        return this.parseDateToJson(key);
    }

    public String lastValue () {
        return this.parseDateToJson(this.datas.lastKey());
    }
}
