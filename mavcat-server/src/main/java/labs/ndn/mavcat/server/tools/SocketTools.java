package labs.ndn.mavcat.server.tools;

import labs.ndn.mavcat.server.connection.LtpAddress;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

public class SocketTools {
    public static void writeAndFlush(Channel channel, String writeStr) {
        channel.writeAndFlush(Unpooled.copiedBuffer(writeStr + "\n", CharsetUtil.UTF_8));
    }

    public static LtpAddress getLtpAddressByChannel(Channel channel) {
        String host = ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
        int port = ((InetSocketAddress) channel.remoteAddress()).getPort();
        return new LtpAddress(host,port);
    }
}
