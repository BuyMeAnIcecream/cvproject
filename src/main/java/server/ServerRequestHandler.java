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
class ServerRequestHandler{
    private static Timer timeout = new HashedWheelTimer();
    private final String nl = "\n"; //new line

    public ByteBuf getResponse(ChannelHandlerContext ctx, HttpRequest req, ServerHandler main) {

        if (req.getUri().startsWith("/redirect?url=")) {
            ServerHandler.sendRedirect(ctx, req.getUri().substring(14) + '/');
            return null;
        }

        if (req.getUri().equals("/hello")) {
            ByteBuf hello = copiedBuffer("<html><body><h1>Hello World</h1></body></html>", CharsetUtil.UTF_8);
            timeout.newTimeout(new DelayedHelloWorld(ctx, req,
                    ServerHandler.formResponse(hello)), 10, TimeUnit.SECONDS);
            main.hello = true; //just to handle the request in ServerHandler now, not in 10 seconds
            return null;
        }

        if ("/favicon.ico".equals(req.getUri())) {
            FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            ServerHandler.sendHttpResponse(ctx, req, res);
            return null; //to return 404
        }

        if ("/status".equals(req.getUri())) {


            return getStatus();
        }

        FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
        ServerHandler.sendHttpResponse(ctx, req, res);

        return null;
    }

    private ByteBuf getStatus(){

        RequestStatistics req = RequestStatistics.getInstance();
        List ipList = req.getIpList();
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

    private class DelayedHelloWorld implements TimerTask {
        private ChannelHandlerContext ctx;
        private FullHttpResponse response;
        private HttpRequest req;

        public DelayedHelloWorld(ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse response) {
            this.ctx = ctx;
            this.response = response;
            this.req = req;
        }

        @Override
        public void run(Timeout t) throws Exception {
            ServerHandler.sendHttpResponse(ctx, req, response);
            ctx.flush();
        }

    }
}