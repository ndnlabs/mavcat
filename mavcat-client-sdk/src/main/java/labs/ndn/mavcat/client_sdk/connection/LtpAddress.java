package labs.ndn.mavcat.client_sdk.connection;

import java.net.InetSocketAddress;

public class LtpAddress extends InetSocketAddress {

    public LtpAddress(String host, int port) {
        super(host,port);
    }

    public String getHost() {
        return getHostString();
    }

}
