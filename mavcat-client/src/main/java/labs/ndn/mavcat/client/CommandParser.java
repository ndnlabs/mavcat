package labs.ndn.mavcat.client;

import labs.ndn.mavcat.client_sdk.connection.ConnectionManager;
import labs.ndn.mavcat.client_sdk.connection.ConnectionUtils;
import labs.ndn.mavcat.client_sdk.connection.LtpTCPConnection;
import labs.ndn.mavcat.client_sdk.download.DownloadListener;
import labs.ndn.mavcat.client_sdk.download.DownloadTask;
import labs.ndn.mavcat.client_sdk.download.DownloadTaskManager;
import labs.ndn.mavcat.client_sdk.tools.StringTools;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class CommandParser {
    private static final String PAUSE = "pause";
    private static final String RESUME = "resume";
    private static final String REMOVE = "remove";

    private static final String[] knownCommands = {"connect","dir","put","stop","pause","resume","set","update" ,"show","get","task"};

    public static final String NO_COMMAND_FOUND = "no command found";

    public static String parseCommand(String line) {
        String[] line_split = line.split(" ");
        String command = line_split[0];
        if (Arrays.asList(knownCommands).contains(command)) {

            String methodName = "parse" + command.substring(0,1).toUpperCase() + command.substring(1);
            try {
                Method method = CommandParser.class.getDeclaredMethod(methodName,String[].class);
                method.setAccessible(true);
                return (String) method.invoke(null,new Object[]{line_split});
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                return NO_COMMAND_FOUND;
            }
        }
        return NO_COMMAND_FOUND;
    }


    private static String parseConnect(String[] line) {
        if (line.length >= 3) {

            String host = line[1];
            int port;
            try {
                port = Integer.parseInt(line[2]);
            }
            catch (NumberFormatException e)
            {
                return "port number must be integer";
            }

            if (!ConnectionUtils.validate(host) || port < 1 || port > 65535) {
                return "server address invalid";
            }


            try {
                ConnectionManager.getInstance().connectServer(host, port,line.length >= 4?line[3]:null);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            return null;

        } else {
            return "use 'connect host_address port password' to connect to server ";
        }
    }

    private static String parseDir(String[] line) {
        LtpTCPConnection tcpConnection = ConnectionManager.getInstance().getActiveTcpConnection();
        if (tcpConnection != null) {
            if (!tcpConnection.isActive()) {
                ConnectionManager.getInstance().removeConnection(tcpConnection.getLtpAddress());
                return "connection interrupted";
            }
            if (line.length == 1) {
                return tcpConnection.getFileList("/").toString();
            } else  {
                return tcpConnection.getFileList(line[1]).toString();
            }
        } else {
            return "no server is connected";
        }
    }

    private static String parseGet(String[] line) {

        LtpTCPConnection tcpConnection = ConnectionManager.getInstance().getActiveTcpConnection();
        if (tcpConnection != null && line.length == 3) {
            String filename = line[1];
            String savePath = line[2];


            DownloadTask task = tcpConnection.downloadFile(filename,savePath);

            task.addDownloadListener(new DownloadListener() {
                @Override
                public void finish() {
                    System.out.println("packet loss is "+task.getPacketLossPercentage()+"%,"+"transfer speed is "+ task.getTotalSpeed()+"Mbps");
                }

                @Override
                public void idReceived() {
                    // at now make sure that remote file exist
                    task.setBandwidth(100);
                }
            });
        }
        return "no server is connect";
    }

    private static String parseUpdate(String[] line) {
        String property = line[1];
        if (property.equals("bandwidth")) {
            int taskId = StringTools.parsePositiveInteger(line[2]);
            if (taskId == -1) {
                return "taskid should be a number";
            }
            // do not check in the current environtment
            DownloadTask task = DownloadTaskManager.getTask(taskId);
            if (task == null) {
                return "no task found";
            } else {
                int bandwidth = StringTools.parsePositiveInteger(line[3]);
                if (bandwidth == -1) {
                    return "bandwidth is not number";
                }
                task.setBandwidth(bandwidth);
                return "notified server to update bandwidth";
            }
        } else {
            return "no property found";
        }
    }

    private static String parsePause(String[] line) {
        return handleTask(line, PAUSE);
    }

    private static String parseResume(String[] line) {
        return handleTask(line,RESUME);
    }


    private static String parseStop(String[] line) {
        return handleTask(line,REMOVE);
    }

    private static String handleTask(String[] line,String type) {
        int taksId = StringTools.parsePositiveInteger(line[1]);
        if (taksId == -1) {
            return "taskid format error";
        }
        DownloadTask task = DownloadTaskManager.getTask(taksId);
        if (task == null) {
            return "no task found";
        }
        if (type.equals(PAUSE)) {
            task.pauseTransfer();
        } else if (type.equals(REMOVE)){
            task.removeTransfer();
        } else if (type.equals(RESUME)){
            task.resumeTransfer();
        }

        return "operation fininshed";
    }



    private static String parseTask(String[] line) {
        return null;
    }

    private static String parsePut(String[] line) {
        return "put";
    }



    private static String parseSet(String[] line) {
        String property = line[1];
        LtpTCPConnection connection = ConnectionManager.getInstance().getActiveTcpConnection();
        if (connection == null) {
            return "no server is connected";
        }
        if (property.equals("speed")) {
            int speed = StringTools.parsePositiveInteger(line[2]);
            connection.setBandwidth(speed);
        }
        return "property set";
    }

    private static String parseShow(String[] line) {
        return "show";
    }
}
