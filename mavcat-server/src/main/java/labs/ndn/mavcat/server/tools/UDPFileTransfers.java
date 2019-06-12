package labs.ndn.mavcat.server.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class UDPFileTransfers {
    private static final Logger logger = LoggerFactory.getLogger(UDPFileTransfers.class);

//    public static void transferFile(int taskId, Channel channel, InetSocketAddress address) throws FileNotFoundException {
//        GetTaskInfo info = ConnectionManager.getTask(taskId);
//        int startBlock = info.getStartBlock();
//        int blockSize = info.getBlockSize();
//        int blockCount = info.getBlockCount();
//
//        String fileName = info.getFilename();
//
//        ByteBuf buf = Unpooled.buffer(info.getBlockSize());
//        RandomAccessFile file = new RandomAccessFile(fileName,"r");
//        byte[] data = new byte[blockSize];
//        for (int i = startBlock; i <= blockCount; i++) {
//            waitSomeTime(info.getBlockSpeed(i),blockSize);
//            buf.clear();
//            try {
//                file.seek(i*blockSize);
//                file.read(buf.array());
//                channel.writeAndFlush(buf);
//            } catch (IOException e) {
//                // do nothing
//            }
//        }
//
//        SocketTools.writeAndFlush(info.getTcpChannel(),"transferend " + taskId);
//
//    }
/*
    public static void reTransfer(int taskId, int[] ids,GetTaskInfo info,Channel channel) {
        RandomAccessFile file;
        try {
            file = new RandomAccessFile(info.getFilename(),"r");
        } catch (FileNotFoundException e) {
            logger.error("error in retransfer to get  file:" + info.getFilename());
            return;
        }
        int blockSize = info.getBlockSize();
        ByteBuf buf = Unpooled.buffer(blockSize);
        for (int block : ids) {
            waitSomeTime(info.getActualSpeed(),info.getBlockSize());
            buf.clear();
            try {
                file.seek(block*blockSize);
                file.read(buf.array());
                channel.writeAndFlush(buf);
            } catch (IOException e) {
                logger.error("error to read file: " + info.getFilename());
            }
        }
    }
*/

    public static float getWaitTime(float speed,int blockSize) {
        long speedInBytes = (long) (speed * 1024 * 1024 / 8);
        float timeSplit = speedInBytes / blockSize;
        return 1 / timeSplit;
    }

    public static void waitSomeTime(float speed, int blockSize) {
        long nanos =(long) (getWaitTime(speed,blockSize) * 1000000000);
        try {
            TimeUnit.NANOSECONDS.sleep(nanos);
        } catch (InterruptedException e) {
            // do nothing
        }
    }


}
