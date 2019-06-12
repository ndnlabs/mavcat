package labs.ndn.mavcat.server.connection;

public class ChannelConfig {
    public static final int DEFAULT_BLOCK_SIZE  = 1024;
    // M
    public static final int DEFAULT_RECEIVE_RATE = 100;

    // generated random string for authentication
    private String random;

    private boolean authed;

    public ChannelConfig(String random) {
        this.random = random;
        this.authed = false;
    }

    public String getRandom() {
        return random;
    }

    public boolean isAuthed() {
        return authed;
    }

    public void setAuthed(boolean authed) {
        this.authed = authed;
    }
}
