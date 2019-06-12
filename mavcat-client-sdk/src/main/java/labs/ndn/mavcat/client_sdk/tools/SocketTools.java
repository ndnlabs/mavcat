package labs.ndn.mavcat.client_sdk.tools;

import labs.ndn.mavcat.client_sdk.connection.LtpAddress;
import labs.ndn.mavcat.client_sdk.handler.MavcatClientVersionHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;

public class SocketTools {
    public static LtpAddress getLtpAddressByChannel(Channel channel) {
        String host = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
        int port = ((InetSocketAddress) channel.remoteAddress()).getPort();
        return new LtpAddress(host,port);
    }

    public static void writeAndFlush(Channel channel,String writeStr) {
        ChannelFuture lastWriteFuture = channel.writeAndFlush(writeStr+"\r\n");
        if (lastWriteFuture != null) {
            try {
                lastWriteFuture.sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendVersionInfo(Channel ch) {
        ch.writeAndFlush(MavcatClientVersionHandler.VERSION + "\r\n");
    }
}
