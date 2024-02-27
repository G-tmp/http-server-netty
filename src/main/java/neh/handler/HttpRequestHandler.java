package neh.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import io.netty.handler.codec.http.multipart.*;
import neh.request.Get;
import neh.request.Request;
import neh.request.Post;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class HttpRequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    private HttpRequest httpRequest;
    private static final HttpDataFactory FACTORY = new DefaultHttpDataFactory(16 * 1024);
    private HttpPostRequestDecoder decoder;
    private Request request ;

//    public Set<Cookie> parseCookie() {
//        String value = this.request.headers().get((CharSequence) HttpHeaderNames.COOKIE);
//        return value == null ? null : ServerCookieDecoder.STRICT.decode(value);
//    }
//
//    public Map<String, List<String>> parseParameters() {
//        QueryStringDecoder decoderQuery = new QueryStringDecoder(this.request.uri());
//        return decoderQuery.parameters();
//    }

    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            this.httpRequest = (HttpRequest)msg;
            String url = URLDecoder.decode(this.httpRequest.uri(), StandardCharsets.UTF_8);
            System.out.print(ctx.channel().remoteAddress() + "\t");
            System.out.print(this.httpRequest.method() + " ");
            System.out.print(url);
            System.out.print("\n");

            // get
            if (this.httpRequest.method().equals(HttpMethod.GET)) {
                this.request = new Get(ctx, this.httpRequest);
                this.request.execute();
                return;
            }

            // post
            if (this.httpRequest.method().equals(HttpMethod.POST)) {
                try {
                    this.decoder = new HttpPostRequestDecoder(FACTORY, this.httpRequest);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException ex) {
                    ex.printStackTrace();
                    return;
                }

                this.request = new Post(ctx, this.httpRequest, this.decoder);
            }
        }

        // post body
        if (this.decoder != null && msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent)msg;

            try {
                this.decoder.offer(chunk);
            } catch (HttpPostRequestDecoder.ErrorDataDecoderException ex) {
                ex.printStackTrace();
                this.request.setContent("500");
                this.request.sendFullResponse(ctx.channel(), 500, true);
                return;
            }

            this.request.execute();

            if (chunk instanceof LastHttpContent) {
                this.request.setContent("Foooooooooo~");
                this.request.sendFullResponse(ctx.channel(), 200, false);
                this.decoder.destroy();
                this.decoder = null;
                this.request.reset();
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

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
    }
}
