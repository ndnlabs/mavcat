package labs.ndn.mavcat.client_sdk.config;

import labs.ndn.mavcat.client_sdk.connection.LtpAddress;

import java.util.HashMap;
import java.util.Map;

public class ConnectionConfig {

    private static final Map<LtpAddress,String> serverPassword = new HashMap<>();

    public static void setServerPassword(LtpAddress address,String password) {
        serverPassword.put(address,password);
    }

    public static String getServerPasswordAndDelete(LtpAddress address) {
        return serverPassword.remove(address);
    }

}
