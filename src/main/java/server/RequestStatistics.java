package server;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Oldoak on 3/6/2015.
 */
public class RequestStatistics {

    static List ipList = Collections.synchronizedList(new LinkedList());


    private Map<String, Long> url = new TreeMap<>(); //url, number of redirects
    private Map<String, Long[]> ip = new TreeMap<>();  //here Long[2]; ip, number of queries, time of last query
    private int activeCon;
    private long totalCon = 1; //Because totalCon is incremented at the moment of sending the response
    private final Object totalConLock = new Object(); //these 2 are used to lock variables in getters
    private final Object activeConLock = new Object();

    public static RequestStatistics req;

    public static RequestStatistics getInstance(){
        if(req == null) {
            req = new RequestStatistics();
        }
        return req;
    }

    void activeConInc(){
        synchronized (activeConLock) {
            activeCon++;
        }
    }

    void activeConDec(){
        synchronized (activeConLock) {
            activeCon--;
        }
    }
    void totalConInc(){
        synchronized (totalConLock) {
            totalCon++;
        }
    }

    public static synchronized void insertElement(LogIp o){

        try{
            synchronized (req.url) {
                if (o.getUrl().startsWith("/redirect?url=")) {
                    String url = o.getUrl().substring(14);
                    if (req.url.containsKey(url)) {
                        req.url.put(url, req.url.get(url) + 1);
                    } else {
                        req.url.put(url, (long) 1);
                    }
                }
            }
        }catch (NullPointerException e ){}

        synchronized (ipList) {

            ipList.add(0, o);    //0,o confused owl

            if (ipList.size() > 16) {
                ipList.remove(16);
            }
        }
    }

    public static synchronized void addIP(String ip, long t) {

            if (req.ip.containsKey(ip)) {
                Long[] i = {req.ip.get(ip)[0] + 1, t};
                req.ip.put(ip, i);
            } else {
                Long[] i = {(long) 1, t};
                req.ip.put(ip, i);
            }
    }
    public static synchronized String stringElement(LogIp o){
        String s = "<tr><th>" + o.getSrcIp()+ "</th><th>"+ o.getUrl()+"</th><th>"+ o.getTimestamp();
        s += "</th><th>"+ o.getSentBytes()+"</th><th>"+ o.getReceivedBytes() + "</th><th>" + o.getSpeed() + "</th></tr>";
        return s;
    }

    public static synchronized String stringAll(List l){
        String response = "";
        for (Object o : l) {
            response += "<tr><th>" + stringElement((LogIp) o) + "</tr></th>";
        }
        return response;
    }

    public static synchronized String stringUrl(Map m) {
        String response = "";
        for (Object o : m.keySet().toArray()) {
            response +=  "<tr><th>" + o + "</th>" + "<th>" + m.get(o).toString()+ "</th><tr>" ;
        }

        return response;
    }

    public static synchronized String stringIP(Map ip) {
        String response = "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        for (Object o : ip.keySet()) {
            Long[] l = (Long[])ip.get(o);
             response += "<tr><th>" + o + "</th><th>" + l[0] + "</th><th>" + sdf.format(new java.util.Date (l[1])) + "</th></tr>";

        }
        return response;
    }

    public synchronized int getActiveCon() {
        return activeCon;
    }

    public synchronized long getTotalCon() {
        synchronized (totalConLock) {
            return totalCon;
        }
    }

    public List getIpList() {
        synchronized (ipList) {
            return ipList;
        }
    }

    public  Map<String, Long> getUrl() {
        synchronized(url) {
            return url;
        }
    }
    public synchronized Map<String, Long[]> getIp() {
        synchronized(ip) {
            return ip;
        }
    }
}
