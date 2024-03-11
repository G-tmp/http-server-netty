package neh.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import neh.request.Post;
import neh.utils.HTMLMaker;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class HttpPostHandler extends SimpleChannelInboundHandler<HttpObject> {
    private HttpRequest request;
    private Post postResponse;
    // upload small file error solution https://github.com/netty/netty/issues/1727
    private static final HttpDataFactory FACTORY = new DefaultHttpDataFactory(true);
    private HttpPostRequestDecoder decoder;

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception{
//        System.out.println(" -post- " + msg);
        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;

            if (!this.request.method().equals(HttpMethod.POST)) {
                ctx.fireChannelRead(msg);
                return;
            }

            String url = URLDecoder.decode(this.request.uri(), StandardCharsets.UTF_8);
            System.out.print(ctx.channel().remoteAddress() + "\t");
            System.out.print(this.request.method() + " ");
            System.out.print(url);
            System.out.print("\n");

            try {
                this.decoder = new HttpPostRequestDecoder(FACTORY, this.request);
            } catch (HttpPostRequestDecoder.ErrorDataDecoderException ex) {
                ex.printStackTrace();
                return;
            }

            postResponse = new Post(ctx, this.request, this.decoder);
        }


        // post body
        if (postResponse != null) {
            if (this.decoder != null && msg instanceof HttpContent) {
                HttpContent chunk = (HttpContent) msg;

                try {
                    this.decoder.offer(chunk);
                    postResponse.execute();
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException | IOException ex) {
//                    ex.printStackTrace();
                    postResponse.setContent(HTMLMaker._500());
                    postResponse.sendFullResponse(ctx.channel(), 500, true);
                }

                if (msg instanceof LastHttpContent) {
                    System.out.print("\n");
                    postResponse.setContent(HTMLMaker.uploadSuccessful());
                    postResponse.sendFullResponse(ctx.channel(), 200, false);
                    postResponse.reset();
                    this.decoder.destroy();
                    this.decoder = null;
                }
            }

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
        if (this.decoder != null) {
            this.decoder.cleanFiles();
        }

        System.err.println("\t" + ctx.channel().remoteAddress() + " inactive");
    }


}