package labs.ndn.mavcat.client_sdk.download;

import labs.ndn.mavcat.client_sdk.thread.ThreadManager;
import labs.ndn.mavcat.client_sdk.tools.FileTools;
import labs.ndn.mavcat.client_sdk.tools.SocketTools;
import labs.ndn.mavcat.client_sdk.connection.LtpTCPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class DownloadTask {
    private static final int BLOCKS_ACCUMULATION_TO_GET_SPEED = 1000;
    private int taskId;
    //
    private String filename;
    private String savePath;
    private int blockSize;
    private int blockCount;
    private String hash;
    private volatile int latestWriteBlock;

    private BitSet blockWriteMarker;
    private BitSet blockReceiveMarker;

    private Map<Integer,byte[]> blocks;

    private RandomAccessFile raf;

    private LtpTCPConnection connection;

    private boolean paused = false;

    // to calculate speed
    private int checkpointBlockAccumulation;
    private long checkpointTime1;
    private long checkpointTime2;

    private long taskCreatetime;
    private long firstTransmitTime;
    private long lastTransmitTime;

    private RandomAccessFile testFile;

    private int prevWriteBlock;

    private int packetLossVolume;

    private boolean finished = false;

    private static final Logger log = LoggerFactory.getLogger(DownloadTask.class);

    private DownloadListener listener;


    public DownloadTask(String filename,String savePath,LtpTCPConnection connection) {
        this.filename = filename;
        this.savePath = savePath;
        this.blocks = new LinkedHashMap<>();
        this.connection = connection;
        blockReceiveMarker = new BitSet(blockCount);
        blockWriteMarker = new BitSet(blockCount);

        createOrLoadBlockMarker();
        this.taskCreatetime = System.currentTimeMillis();
    }

    private void createOrLoadBlockMarker() {
        File file = new File(filename+".downloadinfo");
        if (file.exists()) {
            RandomAccessFile loadInfoRaf;
            try {
                loadInfoRaf = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException e) {
                return;
                // do nothing
            }
            try {
                this.blockCount = loadInfoRaf.readInt();
                this.blockSize = loadInfoRaf.readInt();
                this.latestWriteBlock = loadInfoRaf.readInt();
                int markByteLength = loadInfoRaf.readInt();
                byte[] bitSetByte = new byte[markByteLength];
                loadInfoRaf.readFully(bitSetByte);
                blockWriteMarker = BitSet.valueOf(bitSetByte);
                blockReceiveMarker = BitSet.valueOf(bitSetByte);
            } catch (IOException e) {
                return;
            }
            try {
                loadInfoRaf.close();
            } catch (IOException e) {
                //do nothing
            }
        }
    }

    public float getTransferProgress() {
        return blockReceiveMarker.cardinality()/blockCount;
    }


    public int getTaskId() {
        return taskId;
    }


    public int getBlockSize() {
        return blockSize;
    }


    public void addBlock(int blockNum,byte[] cont) {
        if (firstTransmitTime == 0) {
            firstTransmitTime = System.currentTimeMillis();
        }
        blockReceiveMarker.set(blockNum);
        checkpointForSpeed();
/*        blocks.put(blockNum,cont);
        if (blocks.size() >= 100 || blockNum == blockCount - 1 || blockReceiveMarker.cardinality() > blockCount - 110) {
            Map<Integer,byte[]> readyBlocks = this.blocks;
            handleBlock(readyBlocks);
            this.blocks = new LinkedHashMap<>();
        } */
        writeBlock(blockNum,cont);
     // validateReceiveBlock(blockNum,cont);
    }

    // test if receive block content is correct by look file in another position
    private void validateReceiveBlock(int blockNum,byte[] cont) {
        String filePath = "/Users/vikey/Downloads/AcroRdrDC.dmg";
        if (testFile == null) {
            try {
                testFile = new RandomAccessFile(filePath,"r");
            } catch (FileNotFoundException e) {
                log.error("validate file not found");
                System.exit(-1);
                return;
            }
        }
        byte[] correctCont = new byte[blockSize];
        try {
            testFile.seek(blockNum*blockSize);
            testFile.readFully(correctCont);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!Arrays.equals(cont,correctCont)) {
            log.error("block cont not match:"+blockNum);
        }

    }

    private void checkpointForSpeed() {
        checkpointBlockAccumulation++;
        if (checkpointBlockAccumulation >= BLOCKS_ACCUMULATION_TO_GET_SPEED) {
            checkpointBlockAccumulation = 0;
            checkpointTime1 = checkpointTime2;
            checkpointTime2 = System.nanoTime();
        }
    }

    private void writeBlock(int blockNum,byte[] cont) {
        ThreadManager.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (raf) {

                    try {
                        if (blockNum - prevWriteBlock != 1) {
                            // make result to be long
                            raf.seek(blockNum * (long)blockSize);
                        }
                        raf.write(cont);
                        blockWriteMarker.set(blockNum);
                        prevWriteBlock = blockNum;
                        // write every 1000 block for performance
                        if (blockNum - latestWriteBlock > 1000) {
                            latestWriteBlock = blockNum;
                        }
                    } catch (IOException e) {
                        log.error("io exception during first write block");
                        rewriteBlock(blockNum,cont);
                    }
                }
            }
        });
    }

    // when block write fail
    private void rewriteBlock(int blockNum,byte[] cont) {
        int i = 0;
        boolean writed = false;
        while(!writed) {
            // write fail more than 3 times
            if (i >= 3) {
                log.error("write block fail for 3 times,maybe disk is broken");
                System.exit(-1);
            }

            try {
                raf.seek(blockNum*(long)blockSize);
                raf.write(cont);
                blockWriteMarker.set(blockNum);
                prevWriteBlock = blockNum;
                writed = true;
            } catch (IOException e) {
            }

            i++;
        }

    }

    private void handleBlock(Map<Integer,byte[]> blocks) {
        ThreadManager.submit(new Runnable() {
            @Override
            public void run() {
                writeBlock(blocks);
            }
        });
    }

    private void writeBlock(Map<Integer,byte[]> blocks) {
        synchronized (raf) {
            int preBlock = -10;
            for (Map.Entry<Integer, byte[]> entry : blocks.entrySet()) {
                int blockNum = entry.getKey();

                try {
                        raf.write(entry.getValue());
                        raf.seek(blockNum * blockSize);
                        raf.write(entry.getValue());

                    blockWriteMarker.set(blockNum);
                } catch (IOException e) {
                    log.error("io exception during write block");
                    System.out.println("fetal error ,io exception during write block");
                    blockWriteMarker.clear(entry.getKey());
                    System.exit(-1);
                }
            }
        }

    }


    public String getFilename() {
        return filename;
    }

    public void receiveServerTask(int taskId,int blockCount,String hash) {
        try {
            raf = new RandomAccessFile(filename+".download","rw");
        } catch (FileNotFoundException e) {
            return;
        }
        // this is fresh task

        this.blockCount = blockCount;
        this.hash = hash;

        this.taskId = taskId;

        DownloadTaskManager.putTaskWithId(taskId,this);

        if (listener != null) {
            listener.idReceived();
        }
    }

    // write download info to file
    public void writeDownloadInfo() {
        if (latestWriteBlock <= 0) {
            return;
        }
        try {
            RandomAccessFile infoFile = new RandomAccessFile(this.filename+".downloadinfo","rw");
            infoFile.seek(0);
            infoFile.writeInt(blockCount);
            infoFile.writeInt(blockSize);
            infoFile.writeInt(latestWriteBlock);
            byte[] markByte = blockWriteMarker.toByteArray();
            infoFile.writeInt(markByte.length);
            infoFile.write(markByte);
        } catch (IOException e) {
            // do nothing
        }
    }

    public boolean allBlockReceived() {
        if (blockReceiveMarker.cardinality() == blockCount) {
            if (lastTransmitTime == 0) {
                lastTransmitTime = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    public List<Integer> getUnreceivedBlock() {
        List<Integer> unreceived = new ArrayList<>();
        for (int i = 0; i < blockCount; i++) {
            if (!blockReceiveMarker.get(i)){
                unreceived.add(i);
            }
        }

        // can only calculate the first packet loss
        if (packetLossVolume == 0) {
            packetLossVolume = unreceived.size();
        }
        return unreceived;
    }

    public boolean checkSha256AndRename() {
        // make sure all file content has wrote to disk
        int maxWaitTime = 30;
        while(blockWriteMarker.cardinality() != blockCount) {
            if (maxWaitTime <= 0) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            maxWaitTime--;
        }

        if (blockWriteMarker.cardinality() != blockCount) {
            log.error("can not write block to file after long time");
            return false;
        }

        try {
            raf.close();
            String downloadSha = FileTools.getFileSha256(filename+".download");
             if (downloadSha.equals(hash)) {
                 renameFilename();
                 finishDownload();
                 return true;
             }
             return false;
        } catch (IOException e) {
            log.error("exception during get file sha256");
        }
        return false;
    }

    private void finishDownload() {
        finished = true;
        if (listener != null) {
            listener.finish();
        }
    }

    // after download file
    public void renameFilename() {
        File file = new File(filename + ".download");

        File file2 = new File(filename);

        new File(filename+".downloadinfo").delete();

        long transferTimeUsed = lastTransmitTime - firstTransmitTime;

        boolean success = file.renameTo(file2);

    }

    public float getPacketLossPercentage() {
        return (float)packetLossVolume*100/blockCount;
    }

    public double getTotalSpeed() {
        if (!finished) {
            return 0;
        }
        File file = new File(filename);
        long transferTimeUsed = lastTransmitTime - firstTransmitTime;
        if (file.exists()) {
            return file.length()*8/transferTimeUsed/1e3;
        }
        return 0;
    }

    // tell the server to change bandiwdth
    public void setBandwidth(float bandwidth) {
        StringBuilder sb = new StringBuilder("setbandwidth ");
        sb.append(taskId);
        sb.append(" ");
        sb.append(bandwidth);
        SocketTools.writeAndFlush(connection.getChannel(),sb.toString());
    }

    /**
     * pause transfer
     * we just tell server to stop transfer and keep the task
     * When user want to resume task, just request the server to get the new taskid
     *
     */
    public void pauseTransfer() {
        this.paused = true;
        sendStopTransfer();
    }

    public void resumeTransfer() {
        sendGetMessage();
    }

    public void removeTransfer() {
        sendStopTransfer();
        DownloadTaskManager.removeTask(taskId);
        DownloadTaskManager.removeTask(connection.getChannel(),filename);
        try {
            raf.close();
            FileTools.removeDownloadFile(filename);
        } catch (IOException e) {
           // do nothing
        }
    }

    private void sendStopTransfer() {
        SocketTools.writeAndFlush(connection.getChannel(),"stop "+ taskId);

    }

    public LtpTCPConnection getConnection() {
        return connection;
    }

    public double getSpeed() {
        if (checkpointTime1 != 0 && checkpointTime2 != 0) {
            return (BLOCKS_ACCUMULATION_TO_GET_SPEED*blockSize)*10e3/(checkpointTime2-checkpointTime1);
        }
        return 0;
    }


    public void sendGetMessage() {
        int startByte = latestWriteBlock > 0 ? latestWriteBlock : 0;
        int blockSize = this.blockSize > 0 ? this.blockSize: connection.getConfig().getBlockSize();
        this.blockSize = blockSize;
        float receiveRate = connection.getConfig().getReceiveRate();
        StringBuilder builder = new StringBuilder("get");
        builder.append(" ");
        builder.append(filename);
        builder.append(" ");
        builder.append(startByte);
        builder.append(" ");
        builder.append(blockSize);
        builder.append(" ");
        builder.append(receiveRate);
        builder.append("\n");

        connection.getChannel().writeAndFlush(builder.toString());
    }

    public boolean isFinished() {
        return finished;
    }

    public void addDownloadListener(DownloadListener listener) {
        this.listener = listener;
    }
}
