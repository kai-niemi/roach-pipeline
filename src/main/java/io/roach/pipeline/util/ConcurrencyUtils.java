package io.roach.pipeline.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class ConcurrencyUtils {
    private ConcurrencyUtils() {
    }

    public static void runConcurrentlyAndWait(List<Runnable> tasks) {
        List<CompletableFuture<?>> allFutures = new ArrayList<>();
        tasks.forEach(runnable -> allFutures.add(CompletableFuture.runAsync(runnable)));
        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[] {})).join();
    }
}
