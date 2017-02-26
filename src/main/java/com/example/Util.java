/*
* Created at 19:08 on 10/02/2017
*/
package com.example;

import java.io.Closeable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zzhao
 */
public final class Util {

    public static final int DEFAULT_BUF_LENGTH = 1024 * 4;

    public enum LogLevel {DEBUG, INFO, WARN, ERROR}

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private Util() {
        throw new AssertionError("not for instantiation or inheritance");
    }

    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            error("error closing %s", closeable);
            e.printStackTrace(System.err);
        }
    }

    public static int getIntConfig(String name, int def) {
        return Integer.parseInt(System.getProperty(name, String.valueOf(def)));
    }

    private static void log(LogLevel logLevel, String msg) {
        System.out.printf("%5s %s [%16s] %s%n", logLevel.name(), DTF.format(LocalDateTime.now()),
                Thread.currentThread().getName(), msg);
    }

    public static void debug(String format, Object... objs) {
        log(LogLevel.DEBUG, String.format(format, objs));
    }

    public static void info(String format, Object... objs) {
        log(LogLevel.INFO, String.format(format, objs));
    }

    public static void warn(String format, Object... objs) {
        log(LogLevel.WARN, String.format(format, objs));
    }

    public static void error(String format, Object... objs) {
        log(LogLevel.ERROR, String.format(format, objs));
    }

    public static int unsignedToInt(byte b) {
        return 0xFF & b;
    }

    public static void shutdownAndAwaitTermination(ExecutorService es, String id, int timeoutSeconds) {
        if (es != null) {
            es.shutdown(); // disable new tasks from being submitted
            try {
                if (!es.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    // time out
                    es.shutdownNow(); // cancel currently executing tasks
                    // wait for tasks to respond to being cancelled
                    if (!es.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                        warn("<shutdownAndAwaitTermination> executor did not terminate [%s]", id);
                    }
                }
                info("<shutdownAndAwaitTermination> executor stopped [%s]", id);
            } catch (InterruptedException e) {
                warn("<shutdownAndAwaitTermination> terminating executor interrupted [%s]", id);
                es.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void close(String id, AtomicBoolean flag, CountDownLatch latch, Closeable... resources) {
        if (latch.getCount() == 0) { // already in shutdown process
            Util.warn("<Util> closed %s", id);
            return;
        }

        flag.set(true);
        Util.info("<Util> closing %s", id);
        try {
            latch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Util.warn("<Util> waiting for latch interrupted %s", id);
            Thread.currentThread().interrupt();
        }

        if (resources != null) {
            Arrays.stream(resources).forEach(Util::close);
        }

        Util.info("<Util> closed %s", id);
    }
}
