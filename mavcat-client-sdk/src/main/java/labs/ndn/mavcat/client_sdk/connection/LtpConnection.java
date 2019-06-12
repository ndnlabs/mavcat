package labs.ndn.mavcat.client_sdk.connection;

import io.netty.channel.Channel;

abstract class LtpConnection {
    protected Channel channel;


    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }


    abstract void connect(LtpAddress address);
    abstract void disconnect();
}
