package neh.request;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import neh.Server;
import neh.utils.HTMLMaker;
import neh.utils.RangeRequestParser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

public class Get extends Request {
    public Get(ChannelHandlerContext ctx, HttpRequest request) {
        this.ctx = ctx;
        this.httpRequest = request;
        String fullPath = URLDecoder.decode(request.uri(), StandardCharsets.UTF_8);
        this.path = fullPath.contains("?") ? fullPath.substring(0, fullPath.indexOf("?")) : fullPath;
        this.parseCookie();
        this.parseParameters();
    }

    public void execute() throws IOException {
        File file = new File(Server.HOME, this.path);
        if (!file.exists()) {
            this.responseContent = HTMLMaker._404();
            this.sendFullResponse(this.ctx.channel(), 404, false);
        }else if (!file.canRead()){
            this.responseContent = HTMLMaker._403();
            this.sendFullResponse(this.ctx.channel(), 403, false);
        }else if (file.isDirectory()) {
            if (!path.endsWith("/")){
                redirect(ctx.channel(), path + "/");
                return;
            }

            if (this.parameters != null && this.parameters.containsKey("showHidden")) {
                DefaultCookie cookie;
                if ("true".equals(parameters.remove("showHidden").remove(0))) {
                    cookie = new DefaultCookie("showHidden", "true");
                } else {
                    cookie = new DefaultCookie("showHidden", "false");
                }

                cookie.setPath("/");
                this.redirect(this.ctx.channel(), this.path, cookie);
                return;
            }

            if (this.cookies != null) {
                Iterator cookieIterator = this.cookies.iterator();

                while (cookieIterator.hasNext()) {
                    Cookie cookie = (Cookie) cookieIterator.next();
                    if (cookie.name().equals("showHidden") && cookie.value().equals("true")) {
                        this.responseContent = HTMLMaker.index(URLDecoder.decode(this.path, StandardCharsets.UTF_8), true);
                        this.sendFullResponse(this.ctx.channel(), 200, false);
                        cookie.setMaxAge(0);
                    } else {
                        this.responseContent = HTMLMaker.index(URLDecoder.decode(this.path, StandardCharsets.UTF_8), false);
                        this.sendFullResponse(this.ctx.channel(), 200, false);
                    }
                }

            } else {
                this.responseContent = HTMLMaker.index(URLDecoder.decode(this.path, StandardCharsets.UTF_8), false);
                this.sendFullResponse(this.ctx.channel(), 200, false);
            }
        } else if (file.isFile()) {
            // The FileChannel is closed once the FileRegion was written.
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();
            boolean keepAlive = HttpUtil.isKeepAlive(this.httpRequest);
            String mimeType = Files.probeContentType(file.toPath());
            String range = this.httpRequest.headers().get(HttpHeaderNames.RANGE);

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

                return;
            }

            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileLength);
            response.headers().set(HttpHeaderNames.CONNECTION, keepAlive ? HttpHeaderValues.KEEP_ALIVE : HttpHeaderValues.CLOSE);
            if (mimeType == null) {
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            } else {
                if (mimeType.contains("text")) {
                    mimeType = mimeType + "; charset=UTF-8";
                }

                response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
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
