package labs.ndn.mavcat.server.config;

public class ServerConfig {
    // the root directory will be allowed to download
    private static String dir;

    private static String password;

    private static int udpPort;

    public static String getDir() {
        return dir;
    }

    public static void setDir(String dir) {
        ServerConfig.dir = dir;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        ServerConfig.password = password;
    }

    public static int getUdpPort() {
        return udpPort;
    }

    public static void setUdpPort(int udpPort) {
        ServerConfig.udpPort = udpPort;
    }
}
