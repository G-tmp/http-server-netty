package neh.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import io.netty.handler.codec.http.multipart.*;
import neh.request.Get;
import neh.request.Method;
import neh.request.Post;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


public class HttpRequestHandler extends SimpleChannelInboundHandler<HttpObject> {
    private HttpRequest request;
    private static final HttpDataFactory FACTORY = new DefaultHttpDataFactory(16 * 1024);
    private HttpPostRequestDecoder decoder;
    private Method method ;

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
            this.request = (HttpRequest)msg;
            String url = URLDecoder.decode(this.request.uri(), StandardCharsets.UTF_8);
            System.out.print(ctx.channel().remoteAddress() + "\t");
            System.out.print(this.request.method() + " ");
            System.out.print(url);
            System.out.print("\n");

            // get
            if (this.request.method().equals(HttpMethod.GET)) {
                this.method = new Get(ctx, this.request);
                this.method.execute();
                return;
            }

            // post
            if (this.request.method().equals(HttpMethod.POST)) {
                try {
                    this.decoder = new HttpPostRequestDecoder(FACTORY, this.request);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException ex) {
                    ex.printStackTrace();
                    return;
                }

                this.method = new Post(ctx, this.request, this.decoder);
            }
        }

        // post body
        if (this.decoder != null && msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent)msg;

            try {
                this.decoder.offer(chunk);
            } catch (HttpPostRequestDecoder.ErrorDataDecoderException ex) {
                ex.printStackTrace();
                this.method.setContent("500");
                this.method.sendFullResponse(ctx.channel(), 500, true);
                return;
            }

            this.method.execute();

            if (chunk instanceof LastHttpContent) {
                this.method.setContent("Foooooooooo~");
                this.method.sendFullResponse(ctx.channel(), 200, false);
                this.decoder.destroy();
                this.decoder = null;
                this.method.reset();
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
