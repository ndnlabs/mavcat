package labs.ndn.mavcat.server.retransmit;

import labs.ndn.mavcat.server.connection.ConnectionManager;
import labs.ndn.mavcat.server.connection.GetTaskInfo;
import labs.ndn.mavcat.server.handler.MavcatServerHandler;
import labs.ndn.mavcat.server.tools.SocketTools;
import labs.ndn.mavcat.server.tools.UDPFileTransfers;
import labs.ndn.mavcat.server.transfter.CanStopRunnable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;

public class RetranmitWorker implements CanStopRunnable {

    private BlockingQueue<Integer> retransmit;
    private GetTaskInfo info;
    private static final Logger logger = LoggerFactory.getLogger(RetranmitWorker.class);
    private int taskId;
    private boolean stopped;
    private MavcatServerHandler handler;

    public RetranmitWorker(BlockingQueue<Integer> retranmit,int taskId) {
        this.retransmit = retranmit;
        info = ConnectionManager.getTask(taskId);
        this.taskId = taskId;
    }

    public void stopTask() {
        this.stopped = true;
    }

    @Override
    public void run() {
        if (info == null) {
            logger.error("Taskid is not valid when retransmit");
            return;
        }

        String filename = info.getFilename();
        int blockSize = info.getBlockSize();
        int blockCount = info.getBlockCount();
        RandomAccessFile file;
        ByteBuf buf = Unpooled.buffer(blockSize+4);

        Channel udpChannel = info.getUdpChannel();
        try {
            file = new RandomAccessFile(filename,"r");
        } catch (FileNotFoundException e) {
            logger.error("file not exist:"+filename);
            return;
        }
        byte[] data = new byte[blockSize+4];
     //   InetSocketAddress address = info.getRemoteAddress();
        InetSocketAddress address = null;

        while(true && !stopped) {
            int block;
            try {
                block = retransmit.take();
            } catch (InterruptedException e) {
                continue;
                // do nothing
            }
            if (block == -1) {
                // retransmit to the queue end,
                SocketTools.writeAndFlush(info.getTcpChannel(),"transferend "+taskId);
               // buf.release();
               // break;
                continue;
            }
            UDPFileTransfers.waitSomeTime(info.getActualSpeed(),blockSize);
            if (block == blockCount - 1) {
                try {
                    int lastBlockSize = (int) (file.length() - blockSize * block);
                    ByteBuf lastBuf = Unpooled.buffer(lastBlockSize + 4);
                    byte[] lastData = new byte[lastBlockSize];
                    file.seek(block * blockSize);
                    file.read(lastData);
                    lastBuf.writeInt(block);
                    lastBuf.writeBytes(lastData);
                    udpChannel.writeAndFlush(new DatagramPacket(lastBuf, address));
                } catch (IOException e) {
                    logger.error("exception during read file size");
                }
            } else {
                buf.clear();
                try {
                    file.seek(block * blockSize);
                    file.read(data);
                    buf.writeInt(block);
                    buf.writeBytes(data);
                    buf.retain();
                    udpChannel.writeAndFlush(new DatagramPacket(buf, address));
                } catch (Exception e) {
                    logger.error("exception during send block");
                    // do nothing
                }
            }
        }
    }

    public boolean isStopped() {
        return stopped;
    }
}
