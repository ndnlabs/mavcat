package labs.ndn.mavcat.client_sdk.connection;


import labs.ndn.mavcat.client_sdk.config.ConnectionConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {
    private static ConnectionManager instance;
    private  Map<LtpAddress, LtpTCPConnection> tcpConnnections;
    private LtpTCPConnection activeTcpConnection;

    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    private ConnectionManager() {
        tcpConnnections = new HashMap<>();
    }

    public LtpTCPConnection connectServer(String address,int port,String password) {
        if (password != null) {
            ConnectionConfig.setServerPassword(new LtpAddress(address,port),password);
        }
        LtpAddress ltpAddress = new LtpAddress(address,port);

        LtpTCPConnection connection = tcpConnnections.get(ltpAddress);
        if (connection != null) {
            return connection;
        } else {
            connection = new LtpTCPConnection();
            connection.connect(new LtpAddress(address,port));
            if (connection.getChannel().isActive()) {
                tcpConnnections.put(ltpAddress, connection);
                activeTcpConnection = connection;
            }
            return connection;
        }
    }


    public void setConnectionAuthed(LtpAddress address) {
        LtpTCPConnection connection = tcpConnnections.get(address);

        if (connection != null) {
            connection.setAuthed(true);
            activeTcpConnection = connection;
        }
    }

    public LtpTCPConnection getConnection(LtpAddress address) {
        return tcpConnnections.get(address);
    }

    public void removeConnection(LtpAddress address) {
        LtpTCPConnection connection = tcpConnnections.remove(address);
        if (activeTcpConnection == connection) {
            activeTcpConnection = null;
        }
    }


    public LtpTCPConnection getActiveTcpConnection() {
        return activeTcpConnection;
    }

    public Collection<LtpTCPConnection> getConnections() {
        return tcpConnnections.values();
    }
}
