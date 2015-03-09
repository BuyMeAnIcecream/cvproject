package server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.netty.buffer.Unpooled.copiedBuffer;
/**
 * Created by Oldoak on 3/5/2015.
 */
public class ServerRequestHandler{
    private static Timer timeout = new HashedWheelTimer();

    public ByteBuf getResponse(ChannelHandlerContext ctx, HttpRequest req, ServerMainHandler main) {

        if (req.getUri().startsWith("/redirect?url=")) {
            ServerMainHandler.sendRedirect(ctx, req.getUri().substring(14) + '/');
            return null;
        }

        if (req.getUri().equals("/hello")) {
            ByteBuf hello = copiedBuffer("<html><body><h1>Hello World</h1></body></html>", CharsetUtil.UTF_8);
            timeout.newTimeout(new HelloWorldTimerTask(ctx, req,
                    ServerMainHandler.formResponse(hello)), 10, TimeUnit.SECONDS);

            main.hello = true;

            return null;
        }

        if ("/favicon.ico".equals(req.getUri())) {
            FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            ServerMainHandler.sendHttpResponse(ctx, req, res);
            return null;
        }

        if ("/status".equals(req.getUri())) {


            return getStatus();
        }

        FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
        ServerMainHandler.sendHttpResponse(ctx, req, res);

        return null;
    }

    private ByteBuf getStatus(){

        RequestStatistics req = RequestStatistics.getInstance();
        List ipList = req.getIpList();
        final String nl = "\n"; //new line
        String response = "<html><head><style>" +
                "table{ border-collapse: collapse; }" +
                "table,td,th{ border: 1px solid black; } th{font-weight: normal;}</style></head>" +
                "<body><h3>Statistic " + "</h3>" + "<p>" + "</p>";
        response += "<p>" + "Total requests: " + req.getTotalCon() + "</p>";
        response += "<p>" + "Unique requests: "+ req.getIp().size() + "</p>";
        if(req.getUrl().size() != 0){
            response += "<table> <tr> <th> <b> URL </b> </th> " +
                    "<th> <b> Redirections </b> </th> </tr> " +
                RequestStatistics.stringUrl(req.getUrl()) + "</table><br>";}

        response += "<table> <tr> <th> <b> IP </b> </th>" +
                "<th> <b> Requests </b> </th>" +
                "<th> <b> Time of last query </b> </th> </tr>" +
                RequestStatistics.stringIP(req.getIp()) + "</table>";

        response += "<p> Current connections: " + req.getActiveCon() + "</p>";

        if(req.getIpList().size() != 0) {
            response += "<table> <tr> <th> <b> src_ip </b> </th>" +
                "<th> <b> URL </b> </th>" +
                "<th> <b> timestamp </b> </th>" +
                "<th> <b>sent_bytes </b> </th>" +
                "<th> <b>received_bytes </b> </th>" +
                "<th> <b>speed (bytes/sec) </b> </th> </tr>"
                + RequestStatistics.stringAll(ipList) +"</table>" + nl + nl + nl;
        }
        response += "</body></html>";

        return copiedBuffer(response, CharsetUtil.UTF_8);
    }

    private class HelloWorldTimerTask implements TimerTask {
        private ChannelHandlerContext ctx;
        private FullHttpResponse response;
        private HttpRequest req;

        public HelloWorldTimerTask(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse response) {
            setCtx(ctx);
            setResponse(response);
            setRequest(req);
        }

        @Override
        public void run(Timeout t) throws Exception {
            ServerMainHandler.sendHttpResponse(ctx, req, response);
            ctx.flush();
        }

        public void setCtx(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        public void setResponse(FullHttpResponse response) {
            this.response = response;
        }

        public void setRequest(HttpRequest req) {
            this.req = req;
        }
    }
}