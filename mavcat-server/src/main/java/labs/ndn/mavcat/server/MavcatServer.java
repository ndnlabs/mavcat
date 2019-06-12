package labs.ndn.mavcat.server;

import labs.ndn.mavcat.server.config.ServerConfig;
import labs.ndn.mavcat.server.handler.MavcatServerInitializer;
import labs.ndn.mavcat.server.handler.MavcatServerUdpInitializer;
import labs.ndn.mavcat.server.handler.UdpHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CommandLine.Command(name = "mavcatserver", mixinStandardHelpOptions = true, version = "Mavcatserver 1.0")
public class MavcatServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MavcatServer.class);

    @Option(names = {"-p", "--port"}, description = "tcp port server listen to")
    private Integer port = 31000;

    @Option(names = {"-up", "--udp-port"}, description = "udp port will listen on")
    private Integer udpPort = 32000;

    @Option(names = {"-d", "--dir"},description = "directory will be allowed to download and upload to")
    private String dir;

    @Option(names = {"-P", "--password"},description = "the server password that client_sdk needs to know")
    private String password;


    public void run() {
        start();
    }

    public static void main(String[] args) {
        CommandLine.run(new MavcatServer(), args);

    }


    private void start() {
        checkParameters();

        Thread tcpThread = new Thread(() -> {
            startTCPServer();
        });
        tcpThread.start();

        Thread udpThread = new Thread(() -> {
           startUDPServer();
        });
        udpThread.start();



    }

    private void startTCPServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new MavcatServerInitializer());
            b.bind(port).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void startUDPServerNetty() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();


        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup).channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new MavcatServerUdpInitializer());
            b.option(ChannelOption.SO_SNDBUF, 1048576000);
            ChannelFuture f = b.bind(udpPort).sync();

            Channel channel = f.channel();


            f.channel().closeFuture().await();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }  finally {
            workerGroup.shutdownGracefully();

        }

    }

    private void startUDPServer() {
        DatagramSocket socket;

        try {
            socket = new DatagramSocket(udpPort);
        } catch (IOException e) {
            logger.error("unable to start udp socket,exiting...");
            System.exit(-1);
            return;
        }
        ExecutorService pool = Executors.newFixedThreadPool(10);

        DatagramPacket packet = new DatagramPacket(new byte[4],4);
        while (true) {
            try {
                socket.receive(packet);
            } catch (IOException e) {
                logger.error("io exception when receive udp packet");
            }
            pool.execute(new UdpHandler(packet,socket));
        }

    }

    private void checkParameters() {
        String userDir = System.getProperty("user.dir");
        if (dir == null) {
            dir = userDir;
        } else if (dir.startsWith("./")) {
            dir = userDir + dir.replaceFirst(".","");
        } else if (dir.startsWith("/")) {

        } else {
            dir = userDir + dir;
        }
        if (Files.notExists(Paths.get(dir))) {
            System.out.println("directory not exist");
            System.exit(-1);
        }
        ServerConfig.setDir(dir);
        ServerConfig.setPassword(password);
        ServerConfig.setUdpPort(udpPort);
    }





}