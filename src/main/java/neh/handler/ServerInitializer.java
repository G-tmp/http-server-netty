package neh.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;



public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new HttpServerCodec());
//        pipeline.addLast(new HttpRequestHandler());

        // Must pass on post first, i unable to resolve reference count problem
        pipeline.addLast(new HttpPostHandler());
        pipeline.addLast(new HttpGetHandler());

    }
}
