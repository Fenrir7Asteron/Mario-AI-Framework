package agents.bogdanMCTS.Workers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    public static final int MAX_THREAD_POOL_SIZE = 8;

    public static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);
}
