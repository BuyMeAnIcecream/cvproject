package server;

/**
 * Created by Oldoak on 3/6/2015.
 */
public class LogIp {


    final String srcIp;
    final String url;
    final long timestamp;
    final long sentBytes;
    final long receivedBytes;
    final long speed; //bytes/sec

    LogIp(String s, String u, long t, long se, long re, long sp){
        this.srcIp = s;
        this.url = u;
        this.timestamp = t;
        this.sentBytes = se;
        this.receivedBytes = re;
        this.speed = sp;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public String getUrl() {
        return url;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSentBytes() {
        return sentBytes;
    }

    public long getReceivedBytes() {
        return receivedBytes;
    }

    public long getSpeed() {
        return speed;
    }

}
