package easv.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class BackgroundExecutor {
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private static final ExecutorService IO_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            new BackgroundThreadFactory()
    );
    private static final ExecutorService SCAN_EXECUTOR = Executors.newSingleThreadExecutor(
            new BackgroundThreadFactory()
    );
    private static final ExecutorService PREVIEW_EXECUTOR = Executors.newSingleThreadExecutor(
            new BackgroundThreadFactory()
    );

    private BackgroundExecutor() {
    }

    public static ExecutorService io() {
        return IO_EXECUTOR;
    }

    public static ExecutorService scan() {
        return SCAN_EXECUTOR;
    }

    public static ExecutorService preview() {
        return PREVIEW_EXECUTOR;
    }

    private static final class BackgroundThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "weblager-bg-" + THREAD_COUNTER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
