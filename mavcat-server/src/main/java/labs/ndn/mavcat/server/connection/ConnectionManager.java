package labs.ndn.mavcat.server.connection;

import io.netty.channel.Channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionManager {
    private static Map<LtpAddress, LtpTcpConnection> connections = new HashMap<>();

    private static Map<Channel, ChannelConfig> channelConfigs = new HashMap<>();

    private static Map<Integer, GetTaskInfo> tasks = new HashMap<>();

    private static AtomicInteger taskIdAlloc = new AtomicInteger(0);

    public static void addConnection(LtpAddress address,LtpTcpConnection connection) {
        connections.put(address,connection);
    }

    public static void addChannelConfig(Channel channel, ChannelConfig config) {
        channelConfigs.put(channel,config);
    }

    public static ChannelConfig getChannelConfig(Channel channel) {
        return channelConfigs.get(channel);
    }


    public static int addTask(String filename,String requestFilename,LtpAddress address,int startByte, int block_size, float receiveRate, Channel channel) throws IOException {
        int taskId = taskIdAlloc.addAndGet(1);
        GetTaskInfo info = new GetTaskInfo(filename,requestFilename,address,startByte,block_size,receiveRate,channel);
        tasks.put(taskId,info);
        return taskId;
    }

    public static GetTaskInfo getTask(int taskid) {
        return tasks.get(taskid);
    }

}
