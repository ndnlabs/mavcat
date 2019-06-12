package labs.ndn.mavcat.client_sdk.connection;

import labs.ndn.mavcat.client_sdk.download.DownloadTask;
import labs.ndn.mavcat.client_sdk.download.DownloadTaskManager;
import labs.ndn.mavcat.client_sdk.remotefile.RemoteFileUtil;
import labs.ndn.mavcat.client_sdk.tools.SocketTools;
import labs.ndn.mavcat.client_sdk.handler.MavcatClientInitializer;
import labs.ndn.mavcat.client_sdk.remotefile.RemoteFile;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.List;

public class LtpTCPConnection extends LtpConnection{
    private boolean authed = false;
    private RemoteFileUtil remoteFileUtil;

    private LtpAddress ltpAddress;

    private TcpConnectionConfig config = new TcpConnectionConfig();
    public void connect(LtpAddress ltpAddress) {
        this.ltpAddress = ltpAddress;

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new MavcatClientInitializer());
            // Start the connection attempt.
            channel = b.connect(ltpAddress.getAddress(),ltpAddress.getPort()).sync().channel();
            // fail to start this process in Handler
            SocketTools.sendVersionInfo(channel);
        } catch (InterruptedException e) {
            group.shutdownGracefully();
        }
    }

    public boolean isAuthed() {
        return authed;
    }

    public boolean isActive() {
        return channel.isActive();
    }

    public void setAuthed(boolean authed) {
        this.authed = authed;
        remoteFileUtil = new RemoteFileUtil(this);
    }

    public void disconnect() {
        try {
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public TcpConnectionConfig getConfig() {
        return config;
    }

    public LtpAddress getLtpAddress() {
        return ltpAddress;
    }

    public void getFileList() {
        getFileList("/");
    }

    public List<RemoteFile> getFileList(String path) {
        if (remoteFileUtil != null) {
            return remoteFileUtil.getDir(path);
        }
        return null;
    }

    // do not use cache
    public List<RemoteFile> getFileListNoCache(String path) {
        if (remoteFileUtil != null) {
            return remoteFileUtil.getDirNoCache(path);
        }
        return null;
    }


    public void receiveDirMessage(String msg) {
        if (remoteFileUtil != null) {
            remoteFileUtil.recevieDirMessage(msg);
        }
    }

    public DownloadTask downloadFile(String filename, String savePath) {
        if (!savePath.startsWith("/")) {
            String dir = System.getProperty("user.dir");
            savePath = dir + "/" + savePath;
        }

        File f = new File(savePath);

        if (f.exists() && f.isDirectory()) {
            String name = FilenameUtils.getName(filename);
            if (!savePath.endsWith("/")) {
                savePath = savePath + "/" + name;
            }
        } else if (f.exists() && f.isFile()) {
            // do nothing
        }
        DownloadTask task = DownloadTaskManager.addTask(channel,filename,savePath,this);
        task.sendGetMessage();
        return task;
    }

    public void setBandwidth(int bandwidth) {
        config.setReceiveRate(bandwidth);
    }

    public void setBlockSize(int blockSize) {
        config.setBlockSize(blockSize);
    }
}
