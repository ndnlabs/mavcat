package labs.ndn.mavcat.client_sdk.download;

public interface DownloadListener {
    // when download task is finished
    void finish();
    // when download info received;
    void idReceived();
}
