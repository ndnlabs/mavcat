package labs.ndn.mavcat.server.connection;

import io.netty.channel.Channel;

public class LtpTcpConnection {

    private Channel channel;

    private String random;

    public LtpTcpConnection(Channel channel,String random) {
        this.channel = channel;
        this.random = random;
    }

    public String getRandom() {
        return random;
    }
}
