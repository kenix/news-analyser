/*
* Created at 19:10 on 10/02/2017
*/
package com.example.news.analyser;

import com.example.news.Util;
import com.example.news.domain.News;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zzhao
 */
public class Analyser implements Closeable {

    private static final Set<String> HEADLINES_POSITIVE = new HashSet<>(Arrays.asList(
            "up", "rise", "good", "success", "high", "über"
    ));

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("usage: <port>");
            System.exit(1);
        }
        final int port = Integer.parseInt(args[0]);
        try (
                final Selector selector = Selector.open();
                final Analyser analyser = new Analyser()
        ) {
            final Map<SocketChannel, ByteBuffer> bufByChannel = new HashMap<>(); // single thread server
            final Handler<SelectionKey, IOException> acceptHandler =
                    new ExceptionHandler<>(new AcceptHandler(bufByChannel));
            final Handler<SelectionKey, IOException> readHandler =
                    new ExceptionHandler<>(new ReadHandler(bufByChannel, analyser));

            final ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(port));
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            analyser.start();
            Util.info("<Analyser.main> ready for accept connections on %d", port);

            while (true) {
                selector.select();
                final Set<SelectionKey> keys = selector.selectedKeys();
                for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            acceptHandler.handle(key);
                        } else if (key.isReadable()) {
                            readHandler.handle(key);
                        } // not interested in other ops
                    }
                }
            }
        }
    }

    private final AtomicInteger analyserThreadCount = new AtomicInteger(0);

    private final AtomicLong positiveNewsCount = new AtomicLong(0);

    private final AtomicReference<PriorityBlockingQueue<News>> prioQueueRef = new AtomicReference<>();

    private ExecutorService executorService;

    private ScheduledExecutorService scheduler;

    private PriorityBlockingQueue<News> prioQueueForExchange;

    public Analyser() { // multi-threaded analyser
        this.prioQueueForExchange = new PriorityBlockingQueue<>();
        this.prioQueueRef.set(new PriorityBlockingQueue<>());
    }

    private void start() {
        this.executorService = new ThreadPoolExecutor(1, 5, 30,
                TimeUnit.SECONDS, new LinkedBlockingDeque<>(1024),
                r -> new Thread(r, "analyser-" + this.analyserThreadCount.incrementAndGet()),
                new ThreadPoolExecutor.DiscardPolicy());
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "inspector"));
        this.scheduler.scheduleAtFixedRate(this::inspect, 10, 10, TimeUnit.SECONDS);
    }

    AtomicLong getPositiveNewsCount() {
        return positiveNewsCount;
    }

    AtomicReference<PriorityBlockingQueue<News>> getPrioQueueRef() {
        return prioQueueRef;
    }

    PriorityBlockingQueue<News> getPrioQueueForExchange() {
        return prioQueueForExchange;
    }

    void inspect() {
        Util.info("Positive news in the last 10s: %d", this.positiveNewsCount.getAndSet(0));

        final PriorityBlockingQueue<News> queue = this.prioQueueRef.getAndSet(this.prioQueueForExchange);
        final News[] newsArray = sortNews(queue);
        Arrays.stream(newsArray).limit(3).forEach(news -> Util.info("Top prio news: %s", news));

        queue.clear();
        this.prioQueueForExchange = queue;
    }

    News[] sortNews(PriorityBlockingQueue<News> queue) {
        final News[] newsArray = queue.toArray(new News[queue.size()]);
        Arrays.sort(newsArray, Comparator.reverseOrder());
        return newsArray;
    }

    @Override
    public void close() throws IOException {
        Util.shutdownAndAwaitTermination(this.executorService, 5);
        Util.shutdownAndAwaitTermination(this.scheduler, 5);
    }


    void analyse(SocketChannel sc, ByteBuffer buf) throws IOException {
        buf.flip();
        Optional<News> news;
        do {
            news = Util.parseNews(buf);
            news.ifPresent(n -> this.executorService.submit(() -> this.analyseNews(n)));
        } while (news.isPresent());
        buf.compact();
    }

    void analyseNews(News news) {
        if (isPositiveNews(news)) {
            this.positiveNewsCount.incrementAndGet();
            final PriorityBlockingQueue<News> priorityQueue = this.prioQueueRef.get();
            if (priorityQueue.size() < 3) {
                priorityQueue.offer(news);
            } else {
                if (news.compareTo(priorityQueue.peek()) > 0) {
                    priorityQueue.remove();
                    priorityQueue.offer(news);
                }
            }
        }
    }

    boolean isPositiveNews(News news) {
        final int positiveOnes = (int) countPositiveHeadlines(news.getHeadlines());
        return positiveOnes > news.getHeadlines().size() - positiveOnes;
    }

    private long countPositiveHeadlines(List<String> headlines) {
        return headlines
                .stream()
                .filter(HEADLINES_POSITIVE::contains)
                .count();
    }
}
