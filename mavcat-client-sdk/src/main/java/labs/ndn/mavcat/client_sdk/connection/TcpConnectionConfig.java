package labs.ndn.mavcat.client_sdk.connection;

public class TcpConnectionConfig {
    private int blockSize = 1024;
    private float receiveRate = 100;

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public float getReceiveRate() {
        return receiveRate;
    }

    public void setReceiveRate(float receiveRate) {
        this.receiveRate = receiveRate;
    }
}
