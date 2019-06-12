package labs.ndn.mavcat.server.handler;

import labs.ndn.mavcat.server.connection.ConnectionManager;
import labs.ndn.mavcat.server.connection.GetTaskInfo;
import labs.ndn.mavcat.server.tools.SocketTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class UdpHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(UdpHandler.class);
    private DatagramSocket socket;
    private boolean stopped;
    private BlockingQueue<Integer> retransmit;
    private int taskId;
    private InetAddress address;
    private int port;

    public UdpHandler(DatagramPacket initPacket, DatagramSocket socket) {
        this.socket = socket;
        retransmit = new LinkedBlockingDeque<>();

        byte[] data = initPacket.getData();
        int taskId = ByteBuffer.wrap(data).getInt();
        this.taskId = taskId;
        this.address = initPacket.getAddress();
        this.port = initPacket.getPort();
    }
    @Override
    public void run() {

        if (taskId >0) {
            // let client_sdk wait for some time
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            GetTaskInfo info = ConnectionManager.getTask(taskId);
            // do nothing
            if (info == null) {
                return;
            }

            info.setUdpHandler(this);

            InetAddress sender = this.address;
            // the message is not sent from valid address
            if (!info.getTcpAddress().getHost().equals(sender.getHostAddress())) {
                return;
            }

            info.setRemoteAddress(sender);
            long start = System.nanoTime();
            firstTransmit(info);
            System.out.println(System.nanoTime()-start);

            reTransmit(info);

        } else {
            // maybe receive packets

        }

    }

    private void firstTransmit(GetTaskInfo info) {

        int startBlock = info.getStartBlock();
        int blockSize = info.getBlockSize();
        int blockCount = info.getBlockCount();

        String fileName = info.getFilename();



        BufferedInputStream bufferedFileStream;
        long fileLength;
        try {
            // buffer 10m
            bufferedFileStream = new BufferedInputStream(new FileInputStream(fileName),10485760);
            bufferedFileStream.skip((long)blockSize*startBlock);

            fileLength = new File(fileName).length();
        } catch (IOException e) {
            logger.error("filename:"+fileName+" not exist or skip error");
            return;
        }
        int packetSize = blockSize + 4;
        ByteBuffer buffer = ByteBuffer.wrap(new byte[packetSize]);

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

                    byte[] lastData = new byte[lastBlockSize];
                    ByteBuffer lastBuffer = ByteBuffer.wrap(new byte[lastBlockSize+4]);

                    //        file.seek(blockSize*i);
                    //        file.readFully(lastData);
                    lastBuffer.putInt(i);
                    bufferedFileStream.read(lastData);
                    lastBuffer.put(lastData);

                    socket.send(buildPacket(lastBuffer.array(),lastBuffer.capacity()));
                    SocketTools.writeAndFlush(info.getTcpChannel(),"transferend " + taskId + "\n");
                } catch (IOException e) {
                    logger.error("exception during read file size");
                }
            } else {
                buffer.clear();
                try {
                    //  file.seek(blockSize*i);
                    //  file.readFully(data);
                    bufferedFileStream.read(data);
                    buffer.putInt(i);
                    buffer.put(data);
                    socket.send(buildPacket(buffer.array(),packetSize));
                } catch (Exception e) {
                    logger.error("exception during send block");
                    // do nothing
                }
            }
        }


    }

    public void addReTransmit(int blockNum) {
        try {
            retransmit.put(blockNum);
        } catch (InterruptedException e) {
            logger.error("interrupt when add retransmit queue");
        }
    }

    private void reTransmit(GetTaskInfo info) {

        int blockSize = info.getBlockSize();
        long blockSizeLong = blockSize;
        int blockCount = info.getBlockCount();
        RandomAccessFile file;
        int bufSize = blockSize + 4;
        ByteBuffer buf = ByteBuffer.allocate(bufSize);


        try {
            file = new RandomAccessFile(info.getFilename(),"r");
        } catch (FileNotFoundException e) {
            logger.error("file not exist:"+info.getFilename());
            return;
        }


        byte[] data = new byte[blockSize];

        while(!stopped) {
            int block;
            try {
                block = retransmit.take();
            } catch (InterruptedException e) {
                continue;
                // do nothing
            }
            if (block == -1) {

                SocketTools.writeAndFlush(info.getTcpChannel(),"transferend "+taskId);
                continue;
            }
            waitSomeTime(info.getActualSpeed(),blockSize);
            if (block == blockCount - 1) {
                try {
                    int lastBlockSize = (int) (file.length() - blockSize * block);
                    ByteBuffer lastBuf = ByteBuffer.wrap(new byte[lastBlockSize+4]);
                    byte[] lastData = new byte[lastBlockSize];
                    file.seek(block * blockSizeLong);
                    file.read(lastData);
                    lastBuf.putInt(block);
                    lastBuf.put(lastData);
                    socket.send(buildPacket(lastBuf.array(),lastBlockSize+4));
                } catch (IOException e) {
                    logger.error("exception during read file size");
                }
            } else {
                buf.clear();
                try {
                    file.seek(block * blockSizeLong);
                    file.read(data);
                    buf.putInt(block);
                    buf.put(data);

                    socket.send(buildPacket(buf.array(),bufSize));
                } catch (Exception e) {
                    logger.error("exception during send block");
                }
            }
        }

    }


    private float getWaitTime(float speed,int blockSize) {
        long speedInBytes = (long) (speed * 1024 * 1024 / 8);
        float timeSplit = speedInBytes / blockSize;
        return 1 / timeSplit;
    }

    private void waitSomeTime(float speed, int blockSize) {
        // 50ns is initPacket send time
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

    private DatagramPacket buildPacket(byte[] data,int length) {
        return new DatagramPacket(data,length,this.address,this.port);
    }

    public void stopTransfer() {
        stopped = true;
    }
}