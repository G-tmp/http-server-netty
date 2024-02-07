package neh.request;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import neh.Server;
import neh.utils.HTMLMaker;
import neh.utils.RangeRequestParser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

public class Get extends Method {
    public Get(ChannelHandlerContext ctx, HttpRequest request) throws URISyntaxException {
        this.ctx = ctx;
        this.request = request;
        this.uri = new URI(request.uri());
        this.parseCookie();
        this.parseParameters();
    }

    public void execute() throws IOException {
        File file = new File(Server.HOME, this.uri.getPath());
        if (!file.exists()) {
            this.responseContent = HTMLMaker._404();
            this.sendFullResponse(this.ctx.channel(), 404, true);
        } else if (file.isDirectory()) {
            if (this.parameters != null && this.parameters.containsKey("showHidden")) {
                DefaultCookie cookie;
                if (((String) ((List) this.parameters.get("showHidden")).remove(0)).equals("true")) {
                    cookie = new DefaultCookie("showHidden", "true");
                } else {
                    cookie = new DefaultCookie("showHidden", "false");
                }

                cookie.setPath("/");
                this.redirect(this.ctx.channel(), this.uri.getPath(), cookie);
                return;
            }

            if (this.cookies != null) {
                Iterator cookieIterator = this.cookies.iterator();

                while (cookieIterator.hasNext()) {
                    Cookie cookie = (Cookie) cookieIterator.next();
                    if (cookie.name().equals("showHidden") && cookie.value().equals("true")) {
                        this.responseContent = HTMLMaker.index(URLDecoder.decode(this.uri.getPath(), StandardCharsets.UTF_8), true);
                        this.sendFullResponse(this.ctx.channel(), 200, false);
                        cookie.setMaxAge(0);
                    } else {
                        this.responseContent = HTMLMaker.index(URLDecoder.decode(this.uri.getPath(), StandardCharsets.UTF_8), false);
                        this.sendFullResponse(this.ctx.channel(), 200, false);
                    }
                }

            } else {
                this.responseContent = HTMLMaker.index(URLDecoder.decode(this.uri.getPath(), StandardCharsets.UTF_8), false);
                this.sendFullResponse(this.ctx.channel(), 200, false);
            }
        } else if (file.isFile()) {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            boolean keepAlive = HttpUtil.isKeepAlive(this.request);
            String mimeType = Files.probeContentType(file.toPath());
            String range = this.request.headers().get(HttpHeaderNames.RANGE);

            // 206 partial request
            if (range != null) {
                List<RangeRequestParser.Range> rangeList = RangeRequestParser.parseRanges(range);
                RangeRequestParser.Range range1 =  rangeList.remove(0);
                int start;
                int end;
                if (range1.start == null) {
                    start = (int) fileLength - range1.end;
                    end = (int) fileLength - 1;
                } else {
                    start = range1.start;
                    end = range1.end == null ? (int) fileLength - 1 : range1.end;
                }

                int partialLength = end - start + 1;
                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT);
                response.headers().set(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, partialLength);
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
                response.headers().set(HttpHeaderNames.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileLength));
                this.ctx.write(response);
                this.ctx.write(new DefaultFileRegion(raf.getChannel(), start, partialLength), this.ctx.newProgressivePromise());
                ChannelFuture lastContentFuture = this.ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                if (!keepAlive) {
                    lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                }

            }

            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileLength);
            if (mimeType != null) {
                if (mimeType.contains("text")) {
                    mimeType = mimeType + "; charset=UTF-8";
                }

                response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
            } else {
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            }

            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            } else {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            }

            this.ctx.write(response);
            this.ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), this.ctx.newProgressivePromise());
            ChannelFuture lastContentFuture = this.ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }
        }

    }
}
