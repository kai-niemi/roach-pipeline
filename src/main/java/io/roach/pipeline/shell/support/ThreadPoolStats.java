package io.roach.pipeline.shell.support;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class ThreadPoolStats {
    public static ThreadPoolStats from(ThreadPoolTaskExecutor executor) {
        ThreadPoolStats instance = new ThreadPoolStats();
        instance.corePoolSize = executor.getCorePoolSize();
        instance.poolSize = executor.getPoolSize();
        instance.maximumPoolSize = executor.getMaxPoolSize();
        instance.activeCount = executor.getActiveCount();
        instance.taskCount = executor.getThreadPoolExecutor().getTaskCount();
        instance.largestPoolSize = executor.getThreadPoolExecutor().getLargestPoolSize();
        instance.completedTaskCount = executor.getThreadPoolExecutor().getCompletedTaskCount();
        return instance;
    }

    public int maximumPoolSize;

    public int poolSize;

    public int activeCount;

    public long corePoolSize;

    public long taskCount;

    public int largestPoolSize;

    public long completedTaskCount;
}
