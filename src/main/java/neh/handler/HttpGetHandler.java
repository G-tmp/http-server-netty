package neh.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import neh.request.Get;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class HttpGetHandler extends SimpleChannelInboundHandler<HttpObject> {
    private HttpRequest request;
    private Get get;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
//        System.out.println(" -get- " + msg);
        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;

            if (!this.request.method().equals(HttpMethod.GET)) {
                ctx.fireChannelRead(msg);
            }

            String url = URLDecoder.decode(this.request.uri(), StandardCharsets.UTF_8);
            System.out.print(ctx.channel().remoteAddress() + "\t");
            System.out.print(this.request.method() + " ");
            System.out.print(url);
            System.out.print("\n");

            get = new Get(ctx, this.request);
            get.execute();
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.channel().close();
    }

    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().flush();
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.err.println("\t" + ctx.channel().remoteAddress() + " inactive");
    }

}