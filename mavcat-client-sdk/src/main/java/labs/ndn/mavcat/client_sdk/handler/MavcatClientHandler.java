package labs.ndn.mavcat.client_sdk.handler;

import labs.ndn.mavcat.client_sdk.connection.ConnectionManager;
import labs.ndn.mavcat.client_sdk.connection.LtpAddress;
import labs.ndn.mavcat.client_sdk.connection.LtpTCPConnection;
import labs.ndn.mavcat.client_sdk.download.DownloadTask;
import labs.ndn.mavcat.client_sdk.download.DownloadTaskManager;
import labs.ndn.mavcat.client_sdk.download.UdpConnectionHandler;
import labs.ndn.mavcat.client_sdk.tools.SocketTools;
import labs.ndn.mavcat.client_sdk.tools.StringTools;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MavcatClientHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(MavcatClientHandler.class);
    private ChannelHandlerContext ctx;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.startsWith("task")) {
            handleTask(msg);
        } else if (msg.startsWith("transferend")) {
            handleTransferend(msg);
        } else if (msg.startsWith("dir")) {
            handleDir(msg);
        } else {
            System.out.println(msg);
        }

    }

    private void handleTask(String msg) {
        String[] arr = msg.split(" ");
        String filename = arr[4];
        int taskId = StringTools.parsePositiveInteger(arr[1]);
        if (taskId == -1) {
            logger.error("server return wrong taskid");
            return;
        }

        int udpPort = StringTools.parsePositiveInteger(arr[2]);
        if (udpPort == -1) {
            logger.error("server return wrong udp port");
            return;
        }

        int blockCount = StringTools.parsePositiveInteger(arr[3]);
        if (blockCount == -1) {
            logger.error("server return wrong block count");
            return;
        }


        Channel channel = ctx.channel();
        DownloadTask task = DownloadTaskManager.getTask(channel,filename);
        if (task == null) {
            if (task != null) {
                DownloadTaskManager.removeTask(channel,filename);
            }
            logger.error("no task found");
            return;
        } else {
            task.receiveServerTask(taskId,blockCount,arr[5]);
        }

        LtpAddress serverAddress = SocketTools.getLtpAddressByChannel(channel);
        LtpAddress udpServerAddress = new LtpAddress(serverAddress.getHost(),udpPort);

        new Thread(() -> {
            new UdpConnectionHandler(udpServerAddress,task);
        }).start();
    }

    private void handleTransferend(String msg) {
        String[] arr = msg.split(" ");
        int taskId = StringTools.parsePositiveInteger(arr[1]);
        if (taskId == -1) {
            logger.error(" server return wrong taskid in transferend");
            return;
        }
        DownloadTask task = DownloadTaskManager.getTask(taskId);
        if (task == null) {
            logger.error(" task not found in transferend");
            return;
        }
        if (task.allBlockReceived()) {
            if (task.checkSha256AndRename()) {
                // sure file is downloaded
                DownloadTaskManager.removeTask(ctx.channel(),task.getFilename());
                DownloadTaskManager.removeTask(taskId);
            } else {
                logger.error("sha256 not match while all block received:"+task.getFilename());
            }
            // file transfer end
        } else {
            List<Integer> unReceived = task.getUnreceivedBlock();
            retransmitBlock(unReceived,taskId);
        }
    }

    private void handleDir(String msg) {
        LtpTCPConnection connection = ConnectionManager.getInstance().getConnection(
                SocketTools.getLtpAddressByChannel(ctx.channel())
        );
        if (connection != null) {
            connection.receiveDirMessage(msg);
        }
    }

    private void retransmitBlock(List<Integer> block, int taskId) {
        int i = 0;
        StringBuilder sb = new StringBuilder("r ");
        sb.append(taskId);
        sb.append(" ");
        for (Integer b : block) {
            if (i < 100) {
                sb.append(b);
                sb.append(" ");
                i++;
            } else {
                sb.append("\n");
                ctx.channel().writeAndFlush(sb.toString());
                i = 0;

                sb = new StringBuilder("r ");
                sb.append(taskId);
                sb.append(" ");
            }
        }
        sb.append(-1);
        sb.append("\n");
        ctx.channel().writeAndFlush(sb.toString());
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        this.ctx = ctx;
    }


}
