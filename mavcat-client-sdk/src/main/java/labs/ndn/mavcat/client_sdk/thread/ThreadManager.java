package labs.ndn.mavcat.client_sdk.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadManager {

    public static ThreadPoolExecutor poolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

    public static void submit(Runnable runnable){
        poolExecutor.submit(runnable);
    }
}
