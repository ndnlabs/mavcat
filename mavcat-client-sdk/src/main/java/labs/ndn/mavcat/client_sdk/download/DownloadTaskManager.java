package labs.ndn.mavcat.client_sdk.download;

import labs.ndn.mavcat.client_sdk.connection.LtpTCPConnection;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DownloadTaskManager {

    private static Map<Channel,Map<String,DownloadTask>> tasks = new HashMap<>();
    private static Map<Integer,DownloadTask> tasksById = new HashMap<>();

    public static DownloadTask getTask(Channel channel, String filename) {
        return tasks.get(channel).get(filename);
    }

    public static DownloadTask getTask(int taskId) {
        return tasksById.get(taskId);
    }


    public static void removeTask(int taskId) {
        tasksById.remove(taskId);
    }

    public static DownloadTask addTask(Channel channel, String filename, String savePath, LtpTCPConnection connection) {
        DownloadTask task = new DownloadTask(filename,savePath,connection);
        Map<String,DownloadTask> subTasks = tasks.get(channel);
        if (subTasks == null) {
            subTasks = new HashMap<>();
            tasks.put(channel,subTasks);
        }
        subTasks.put(filename,task);
        return task;
    }

    public static void removeTask(Channel channel,String filename) {
        Map<String,DownloadTask> subTasks = tasks.get(channel);
        if (subTasks != null) {
            subTasks.remove(filename);
        }
    }

    public static void putTaskWithId(int taskId,DownloadTask task) {
        tasksById.put(taskId,task);
    }

    public static void scheduleWriteDownloadInfo() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (DownloadTask task: tasksById.values()) {
                    task.writeDownloadInfo();
                }
            }
        },3,5, TimeUnit.SECONDS);

    }

    // clear task that finished for a period and task that not valid
    public static void scheduleClearOutdatedTask() {

    }
}
