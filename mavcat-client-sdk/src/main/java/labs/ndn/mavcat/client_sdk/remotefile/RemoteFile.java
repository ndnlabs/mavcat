package labs.ndn.mavcat.client_sdk.remotefile;



public class RemoteFile {
    private String filename;
    private boolean isDiretory;
    private long fileSize;

    public RemoteFile(String filename, boolean isDiretory, long fileSize) {
        this.filename = filename;
        this.isDiretory = isDiretory;
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return filename;
    }
}
