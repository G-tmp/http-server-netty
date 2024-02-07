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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;

public class Post extends Method {
    private HttpPostRequestDecoder decoder;
    private HttpData partialContent;

    public Post(ChannelHandlerContext ctx, HttpRequest request, HttpPostRequestDecoder decoder) throws URISyntaxException {
        this.ctx = ctx;
        this.request = request;
        this.decoder = decoder;
        this.uri = new URI(request.uri());
    }

    public void execute() throws IOException {
        System.out.println("upload");
        this.readHttpDataChunkByChunk();
    }

    private void readHttpDataChunkByChunk() {
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


    private void writeHttpData(InterfaceHttpData data) {
        if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
            Attribute attribute = (Attribute)data;
            String value = null;

            try {
                value = attribute.getValue();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            System.out.println(value);
        } else if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload)data;
            if (fileUpload.isCompleted()) {
                this.responseContent = "done";
                File file = new File(new File(Server.HOME, this.uri.getPath()), fileUpload.getFilename());

                try (FileChannel inputChannel = (new FileInputStream(fileUpload.getFile())).getChannel();
                     FileChannel outputChannel = (new FileOutputStream(file)).getChannel()) {

                    outputChannel.transferFrom(inputChannel, 0L, inputChannel.size());

                } catch (FileNotFoundException e1) {
                    e1.printStackTrace();
                } catch (IOException e2) {
                    e2.printStackTrace();
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