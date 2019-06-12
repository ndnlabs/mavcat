package labs.ndn.mavcat.server.transfter;

import labs.ndn.mavcat.server.connection.ConnectionManager;
import labs.ndn.mavcat.server.connection.GetTaskInfo;
import labs.ndn.mavcat.server.tools.SocketTools;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;

public class FirstTransmitWorker implements CanStopRunnable {
    private static final Logger logger = LoggerFactory.getLogger(FirstTransmitWorker.class);

    private int taskId;
    private ChannelHandlerContext ctx;
    private InetSocketAddress address;
    private boolean stopped = false;

    public FirstTransmitWorker(int taskId, ChannelHandlerContext ctx, InetSocketAddress address) {
        this.taskId = taskId;
        this.ctx = ctx;
        this.address = address;
    }

    public void stopTask() {
        this.stopped = true;
    }

    @Override
    public void run() {

        GetTaskInfo info = ConnectionManager.getTask(taskId);
        int startBlock = info.getStartBlock();
        int blockSize = info.getBlockSize();
        int blockCount = info.getBlockCount();

        String fileName = info.getFilename();

       // ByteBuf buf = Unpooled.buffer(info.getBlockSize());
        ByteBuf buf = ctx.alloc().buffer(info.getBlockSize()+4);
        RandomAccessFile file = null;
        InputStream fileStream ;
        long fileLength;
        try {
           // file = new RandomAccessFile(fileName,"r");
            fileStream = new FileInputStream(fileName);
            fileLength = new File(fileName).length();
        } catch (FileNotFoundException e) {
            logger.error("filename:"+fileName+" not exist");
            return;
        }
        byte[] data = new byte[blockSize];
        for (int i = startBlock; i < blockCount; i++) {
            if (stopped){
                return;
            }
            waitSomeTime(info.getBlockSpeed(i),blockSize);
            // last block
            if (i == blockCount-1) {
                try {
                    int lastBlockSize =(int) (fileLength - blockSize * i);
                    ByteBuf lastBuf = ctx.alloc().buffer(lastBlockSize+4);
                    byte[] lastData = new byte[lastBlockSize];
            //        file.seek(blockSize*i);
            //        file.readFully(lastData);
                    fileStream.read(lastData);
                    lastBuf.writeInt(i);
                    lastBuf.writeBytes(lastData);
                    ctx.writeAndFlush(new DatagramPacket(lastBuf, address));
                } catch (IOException e) {
                    logger.error("exception during read file size");
                }
            } else {
                buf.clear();
                try {
                  //  file.seek(blockSize*i);
                  //  file.readFully(data);
                    fileStream.read(data);
                    buf.writeInt(i);
                    buf.writeBytes(data);
                    buf.retain();
                    ctx.writeAndFlush(new DatagramPacket(buf, address));
                } catch (Exception e) {
                    logger.error("exception during send block");
                    // do nothing
                }
            }
        }
        buf.release();

        SocketTools.writeAndFlush(info.getTcpChannel(),"transferend " + taskId + "\n");
    }

    private float getWaitTime(float speed,int blockSize) {
        long speedInBytes = (long) (speed * 1024 * 1024 / 8);
        float timeSplit = speedInBytes / blockSize;
        return 1 / timeSplit;
    }

    private void waitSomeTime(float speed, int blockSize) {
        // 50ns is packet send time
        long nanos =(long) (getWaitTime(speed,blockSize) * 1000000000);
        busyWait(nanos);
    }

    public void busyWait(long nanos){
        long start = System.nanoTime();
        long end=0;
        do{
            end = System.nanoTime();
        }while(start + nanos >= end);
    }
}
