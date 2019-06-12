package labs.ndn.mavcat.client_sdk.download;

import labs.ndn.mavcat.client_sdk.connection.LtpAddress;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class UdpConnectionHandler1 {
    private static final Logger logger = LoggerFactory.getLogger(UdpConnectionHandler.class);

    private Channel channel;



    public UdpConnectionHandler1(LtpAddress address,DownloadTask task) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioDatagramChannel.class).handler(new ChannelInitializer<NioDatagramChannel>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) throws Exception {
                ch.pipeline().addLast(new MyUdpDecoder(address,task));
            }
        });
        bootstrap.option(ChannelOption.SO_RCVBUF, 104857600);
        try {
            channel = bootstrap.bind(0).sync().channel();
        } catch (InterruptedException e) {
            logger.error("binding port get interrupted");
        }
    }


    private static class MyUdpDecoder extends SimpleChannelInboundHandler<DatagramPacket> {

        private InetSocketAddress address;
        private DownloadTask task;

        public MyUdpDecoder(LtpAddress address,DownloadTask task) {
            this.address = new InetSocketAddress(address.getHost(),address.getPort());
            this.task = task;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) {
            ByteBuf buf = msg.content();
            int block = buf.readInt();
            byte[] content = new byte[buf.readableBytes()];
            buf.readBytes(content);
            task.addBlock(block,content);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            ByteBuf buf = ctx.alloc().buffer(4);
            buf.writeInt(task.getTaskId());
            DatagramPacket packet = new DatagramPacket(buf,address);
            ctx.channel().writeAndFlush(packet).sync();

        }
    }
}
