package server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.*;

import static io.netty.buffer.Unpooled.copiedBuffer;


/**
 * Created by Oldoak on 3/5/2015.
 */


class ServerHandler extends ChannelInboundHandlerAdapter{

    private long timer;
    private long readTimer;
    private RequestStatistics req;
    private String ip;
    private String url;

    private long sentBytes;
    private long receivedBytes;
    public boolean hello = false;


    public ServerHandler(String ip){
        req = RequestStatistics.getInstance();
        this.ip = ip;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
        timer = System.currentTimeMillis();
        req.activeConInc();

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        req.activeConDec();

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if(!(msg instanceof HttpRequest))
            return;

        receivedBytes += msg.toString().length();
        HttpRequest req = (HttpRequest) msg;
        url = req.getUri();

        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // Allow only GET methods.
        if (req.getMethod() != HttpMethod.GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
            return;
        }

        readTimer = System.currentTimeMillis();
        RequestStatistics.addIP(ip, readTimer);
        ByteBuf content = new ServerRequestHandler().getResponse(ctx, req, this);

        if(hello){
            sentBytes = 46; //weight of the page must be calculated now, but TimerTask is used.
            // The page will gain weight in 10 secs only
        }
        if(content != null) {
                FullHttpResponse res = formResponse(content);
                sentBytes = res.content().writerIndex();
                sendHttpResponse(ctx, req, res);
        }

    }

    public static FullHttpResponse formResponse(ByteBuf content){
        FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        res.headers().set("Content-type", "text/html; charset=UTF-8");
        HttpHeaders.setContentLength(res, content.readableBytes());
        return res;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        long elapsed = System.currentTimeMillis() - timer;
        if(elapsed ==0)elapsed =1;
        long speed = (receivedBytes +sentBytes)*1000/elapsed;
        if(url!=null)
            RequestStatistics.insertElement(new LogIp(ip, url, readTimer, sentBytes, receivedBytes, speed));   //to stack
        ctx.flush();
    }


    public static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set("Location", "http://" + newUri);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void sendHttpResponse(
            ChannelHandlerContext ctx, HttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.getStatus().code() != 200) {
            ByteBuf buf = copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpHeaders.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }

        RequestStatistics.getInstance().totalConInc();
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }



}
