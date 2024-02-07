package neh.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Method {
    ChannelHandlerContext ctx;
    HttpRequest request;
    String responseContent;
    URI uri;
    Set<Cookie> cookies;
    Map<String, List<String>> parameters;

    public abstract void execute() throws IOException;

    public void setContent(String content) {
        this.responseContent = content;
    }

    public void parseCookie() {
        String value = this.request.headers().get( HttpHeaderNames.COOKIE);
        if (value != null) {
            this.cookies = ServerCookieDecoder.STRICT.decode(value);
        }

    }

    public void parseParameters() {
        QueryStringDecoder decoderQuery = new QueryStringDecoder(this.request.uri());
        this.parameters = decoderQuery.parameters();
    }

    public void sendFullResponse(Channel channel, int code, boolean closeConnection) {
        ByteBuf buf = Unpooled.copiedBuffer(this.responseContent, CharsetUtil.UTF_8);
        boolean keepAlive = HttpUtil.isKeepAlive(this.request) && !closeConnection;
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(code));
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONNECTION, (keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE));
        response.content().writeBytes(buf);
        ChannelFuture future = channel.write(response);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

    }

    public void redirect(Channel channel, String location, Cookie cookie) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, location);
        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
        ChannelFuture future = channel.write(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    public void reset() {
        this.request = null;
    }
}
