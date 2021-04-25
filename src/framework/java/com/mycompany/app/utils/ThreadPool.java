package com.mycompany.app.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    public static final int MAX_THREAD_POOL_SIZE = 16;

    public static final ExecutorService parallelGamesThreadPool = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);
}
