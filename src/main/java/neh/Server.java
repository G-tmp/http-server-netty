package neh;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import neh.handler.ServerInitializer;

public class Server {
    private static final int DEFAULT_PORT = 9999;
    private final int port;
    public static final String HOME = System.getProperty("user.home");

    public Server(int port) {
        this.port = port;
    }

    public void launch() {
        System.out.println("neh bind port " + this.port);
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        (bootstrap.group(boss, worker).channel(NioServerSocketChannel.class)).childHandler(new ServerInitializer());

        try {
            Channel channel = bootstrap.bind(this.port).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }

    }

    public static int getValidPort(String[] args) throws NumberFormatException {
        if (args.length > 0) {
            int port = Integer.parseInt(args[0]);
            if (port > 0 && port < 65535) {
                return port;
            } else {
                throw new NumberFormatException("Invalid port! Port value is a number between 0 and 65535");
            }
        } else {
            return DEFAULT_PORT;
        }
    }


    public static void main(String[] args) {
        Server server = new Server(getValidPort(args));
        server.launch();
    }
}
