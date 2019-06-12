package labs.ndn.mavcat.server.connection;

public class LtpAddress {
    private String host;
    private int port;

    public LtpAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        LtpAddress compare = (LtpAddress) obj;
        return compare.getHost().equals(host) && compare.getPort() == port;
    }
}
