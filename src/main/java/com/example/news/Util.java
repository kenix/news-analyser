/*
* Created at 19:08 on 10/02/2017
*/
package com.example.news;

import com.example.news.domain.News;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zzhao
 */
public class Util {

    public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static final int MAX_NEWS_LENGTH = 255;

    public enum LogLevel {DEBUG, INFO, WARN, ERROR}

    private Util() {
        throw new AssertionError("not for instantiation or inheritance");
    }

    public static void log(LogLevel logLevel, String msg) {
        System.out.printf("%s %s [%16s] %s%n", logLevel.name(), DTF.format(LocalDateTime.now()),
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

    public static void shutdownAndAwaitTermination(ExecutorService es, int timeoutSeconds) {
        if (es != null) {
            es.shutdown(); // disable new tasks from being submitted
            try {
                if (!es.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    // time out
                    es.shutdownNow(); // cancel currently executing tasks
                    // wait for tasks to respond to being cancelled
                    if (!es.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                        warn("<shutdownAndAwaitTermination> executor service did not terminate");
                    }
                }
                info("<shutdownAndAwaitTermination> executor service stopped");
            } catch (InterruptedException e) {
                warn("<shutdownAndAwaitTermination> terminating executor service interrupted", e);
                es.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
