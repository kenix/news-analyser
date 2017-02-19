/*
* Created at 19:08 on 10/02/2017
*/
package com.example;

import com.example.news.domain.News;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zzhao
 */
public final class Util {

    public static final int MAX_NEWS_LENGTH = 255;

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

    /**
     * Encoding length:
     * <ol>
     * <li>1 byte for total length</li>
     * <li>1 byte for priority</li>
     * <li>1 byte for headline count</li>
     * <li>for each headline: 1 byte for headline length plus the headline itself</li>
     * </ol>
     *
     * @param news
     * @return encoding length for the given news in number of bytes
     */
    public static int encodingLength(News news) {
        return 1// 1 byte for total length
                + 1 // 1 byte for priority
                + 1 // 1 byte for headline count
                + news.getHeadlines().size()  // 1 byte for each headline length
                + news.getHeadlines()
                .stream()
                .mapToInt(String::length)
                .sum(); // n bytes for total headline length
    }

    /**
     * Encoding schema:
     * <pre>
     *     [total length of news] - 1 byte
     *     [news priority] - 1 byte
     *     [headline counts] - 1 byte
     *     for each headline:
     *          [headline length] - 1 byte
     *          [headline] - (headline length) bytes
     * </pre>
     *
     * @param buf
     * @param news
     * @return encoding length of the given news, 0 if the given buf cannot hold the given news
     */
    public static int encodeNews(ByteBuffer buf, News news) {
        final int length = encodingLength(news);
        if (buf.remaining() < length) {
            return 0;
        }
        buf.put((byte) length); // 1 byte for total length, max 255 bytes
        buf.put((byte) news.getPriority()); // 1 byte for priority
        buf.put((byte) news.getHeadlines().size()); // 1 byte for headline counts
        news.getHeadlines().forEach(hl -> {
            buf.put((byte) hl.length()); // 1 byte for headline length
            for (int i = 0; i < hl.length(); i++) {
                final char c = hl.charAt(i);
                if (c > 255) { // maximum of unsigned byte
                    throw new IllegalArgumentException(c + " at pos. " + i + " > 255");
                }
                buf.put((byte) c);
            }
        });
        return length;
    }

    /**
     * Parse a news object from the given buf if possible.
     *
     * @param buf
     * @return a news optional, empty if no news can be parsed from the given buf
     */
    public static Optional<News> parseNews(ByteBuffer buf) {
        if (!buf.hasRemaining()) {
            return Optional.empty();
        }
        buf.mark();
        final int totalLength = unsignedToInt(buf.get());
        if (buf.remaining() < totalLength - 1) {
            // news fragment
            buf.reset();
            return Optional.empty();
        }
        return Optional.of(new News(buf.get(), parseHeadlines(buf)));
    }

    private static List<String> parseHeadlines(ByteBuffer buf) {
        final int headlineCount = buf.get();
        final ArrayList<String> headlines = new ArrayList<>(headlineCount);
        for (int i = 0; i < headlineCount; i++) {
            headlines.add(parseHeadLine(buf));
        }
        return headlines;
    }

    private static String parseHeadLine(ByteBuffer buf) {
        final int headlineLength = buf.get();
        final StringBuilder sb = new StringBuilder(headlineLength);
        for (int i = 0; i < headlineLength; i++) {
            sb.append((char) unsignedToInt(buf.get()));
        }
        return sb.toString();
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
            Util.warn("<Util> in closing %s", id);
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

        Arrays.stream(resources).forEach(Util::close);
        Util.info("<Util> closed %s", id);
    }
}
