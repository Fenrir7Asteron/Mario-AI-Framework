package com.mycompany.app.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    public static final int MAX_THREAD_POOL_SIZE = 1;

    public static final ExecutorService nodeCalculationsThreadPool = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);
    public static final ExecutorService parallelGamesThreadPool = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);
}
