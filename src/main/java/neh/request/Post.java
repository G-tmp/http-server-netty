package neh.request;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import neh.Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class Post extends Request {
    private HttpPostRequestDecoder decoder;
    private HttpData partialContent;

    public Post(ChannelHandlerContext ctx, HttpRequest request, HttpPostRequestDecoder decoder) {
        this.ctx = ctx;
        this.httpRequest = request;
        this.decoder = decoder;
        String fullPath = URLDecoder.decode(request.uri(), StandardCharsets.UTF_8);
        this.path = fullPath.contains("?") ? fullPath.substring(0, fullPath.indexOf("?")) : fullPath;
    }

    public void execute() throws IOException {
//        System.out.print("▰");
        this.readHttpDataChunkByChunk();
    }

    private void readHttpDataChunkByChunk() throws IOException {
        if (this.decoder != null) {
            try {
                while(this.decoder.hasNext()) {
                    InterfaceHttpData data = this.decoder.next();
                    if (data != null) {
                        if (this.partialContent == data) {
                            this.partialContent = null;
                        }
                        this.writeHttpData(data);
                    }
                }
            } catch (HttpPostRequestDecoder.EndOfDataDecoderException ex) {
                ex.printStackTrace();
            }

        }
    }


    private void writeHttpData(InterfaceHttpData data) throws IOException {
        if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload)data;
            if (fileUpload.isCompleted()) {
                File file = new File(new File(Server.HOME, this.path), fileUpload.getFilename());
                if (file.exists()){
                    responseContent = "overwrite file";
                }else {
                    this.responseContent = "new file";
                }

                try (FileChannel inputChannel = (new FileInputStream(fileUpload.getFile())).getChannel();
                     FileChannel outputChannel = (new FileOutputStream(file)).getChannel()) {

                    outputChannel.transferFrom(inputChannel, 0L, inputChannel.size());

                } catch (IOException exception) {
                    throw exception;
                }
            } else {
                System.out.println("no comp");
            }
        }

    }

    public void reset() {
        super.reset();
    }
}
