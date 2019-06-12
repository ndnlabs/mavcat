package labs.ndn.mavcat.server.connection;

import labs.ndn.mavcat.server.handler.UdpHandler;
import labs.ndn.mavcat.server.tools.FileTools;
import labs.ndn.mavcat.server.transfter.CanStopRunnable;
import io.netty.channel.Channel;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class GetTaskInfo {
    private static final int SPPED_TEST_BASE = 1000;
    private String filename;
    private String requestFilename;
    private LtpAddress tcpAddress;
    private int udpPort;
    private int startBlock;
    private int blockSize;
    private String hash;
    // the target transfer speed
    private float receiveRate;
    private Channel tcpChannel;
    private Channel udpChannel;
    private int blockCount;

    private boolean enableSpeedTest;
    private int speedTestStartBlock;
    // the actual transfer speed
    private float actualSpeed;
    private InetAddress remoteAddress;

    private CanStopRunnable transferTask;
    private CanStopRunnable retransferTask;

    private UdpHandler udpHandler;


    public GetTaskInfo(String filename, String requestFilename, LtpAddress tcpAddress, int startBlock, int blockSize, float receiveRate, Channel tcpChannel) throws IOException {
        this.filename = filename;
        this.requestFilename = requestFilename;
        this.tcpAddress = tcpAddress;
        this.startBlock = startBlock;
        this.blockSize = blockSize;
        this.receiveRate = receiveRate;
        this.tcpChannel = tcpChannel;
        File file = new File(filename);
        this.blockCount = (int)Math.ceil(file.length()/(float)blockSize);
        this.hash = FileTools.getFileSha256(filename);
        this.actualSpeed = receiveRate;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public LtpAddress getTcpAddress() {
        return tcpAddress;
    }

    public String getFilename() {
        return filename;
    }

    public int getStartBlock() {
        return startBlock;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public String getHash() {
        return hash;
    }

    public float getReceiveRate() {
        return receiveRate;
    }

    public float getBlockSpeed(int block) {
        return receiveRate;
  /*      if (enableSpeedTest) {
            if (block > speedTestStartBlock && block <= (speedTestStartBlock + SPPED_TEST_BASE)) {
                return getPercentageSpeed(0.9);
            } else if (block > (speedTestStartBlock + SPPED_TEST_BASE) && block <= (speedTestStartBlock + SPPED_TEST_BASE*2)) {
                return getPercentageSpeed(0.8);
            } else if (block > (speedTestStartBlock + SPPED_TEST_BASE*2) && block <= (speedTestStartBlock + SPPED_TEST_BASE*3)) {
                return getPercentageSpeed(0.7);
            } else if (block > (speedTestStartBlock + SPPED_TEST_BASE*3) && block <= (speedTestStartBlock + SPPED_TEST_BASE*4)) {
                return getPercentageSpeed(0.6);
            } else if (block > (speedTestStartBlock + SPPED_TEST_BASE*4) && block <= (speedTestStartBlock + SPPED_TEST_BASE*5)) {
                return getPercentageSpeed(0.5);
            }
        }
        return actualSpeed; */
    }



    private float getPercentageSpeed(double ratio) {
        return Math.round(receiveRate * ratio * 100) / 100;
    }

    public float getActualSpeed() {
        return actualSpeed;
    }

    public Channel getUdpChannel() {
        return udpChannel;
    }

    public Channel getTcpChannel() {
        return tcpChannel;
    }

    public void setUdpChannel(Channel udpChannel) {
        this.udpChannel = udpChannel;
    }

    public void setReceiveRate(float receiveRate) {
        this.receiveRate = receiveRate;
    }

    public void setTransferTask(CanStopRunnable transferTask) {
        this.transferTask = transferTask;
    }

    public CanStopRunnable getTransferTask() {
        return transferTask;
    }

    public String getRequestFilename() {
        return requestFilename;
    }

    public void setRemoteAddress(InetAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }

    public CanStopRunnable getRetransferTask() {
        return retransferTask;
    }

    public void setRetransferTask(CanStopRunnable retransferTask) {
        this.retransferTask = retransferTask;
    }

    public UdpHandler getUdpHandler() {
        return udpHandler;
    }

    public void setUdpHandler(UdpHandler udpHandler) {
        this.udpHandler = udpHandler;
    }
}
