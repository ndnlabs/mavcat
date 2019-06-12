package labs.ndn.mavcat.client_sdk.remotefile;

import labs.ndn.mavcat.client_sdk.connection.LtpTCPConnection;
import labs.ndn.mavcat.client_sdk.tools.SocketTools;
import labs.ndn.mavcat.client_sdk.tools.StringTools;

import java.util.ArrayList;
import java.util.List;

public class RemoteFileUtil {
    private List<RemoteFile> fileList;

    private LtpTCPConnection tcpConnection;

    private String path;

    private long getResultTime;

    public RemoteFileUtil(LtpTCPConnection tcpConnection) {
        this.tcpConnection = tcpConnection;
        fileList = new ArrayList<>();
    }

    public void getRoot() {
       getDir("/");
    }

    public List<RemoteFile> getDir(String path) {
        if (path.equals(this.path) && !fileList.isEmpty()) {
            return fileList;
        } else {
            return getDirNoCache(path);
        }
    }

    public List<RemoteFile> getDirNoCache(String path) {
        long now = System.nanoTime();
        SocketTools.writeAndFlush(tcpConnection.getChannel(), "dir " + path);
        int i = 5;
        while (i > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
            if (getResultTime > now) {
                if (path.equals(this.path)) {
                    return fileList;
                }
            }
            i--;

        }
        // failed to get dir or time out;
        return null;
    }

    public void recevieDirMessage(String dir) {

        fileList.clear();

        String[] split = dir.split(" ");
        path = split[1];

        for (int i = 2; i < split.length; i++) {
            String[] oneSplit = split[i].split(",");
            fileList.add(new RemoteFile(oneSplit[0],oneSplit[1] == "0"?false:true, StringTools.parsePositiveLong(oneSplit[2])));
        }
        getResultTime = System.nanoTime();
    }

    public long getGetResultTime() {
        return getResultTime;
    }
}
