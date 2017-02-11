/*
* Created at 22:40 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.news.Util;
import com.example.news.domain.News;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsAnalysingInspector implements Consumer<News>, Closeable {

    private final AtomicLong newsCounter = new AtomicLong(0);

    private final AtomicReference<PriorityBlockingQueue<News>> prioQueueRef = new AtomicReference<>();

    private PriorityBlockingQueue<News> prioQueueForExchange;

    private ScheduledExecutorService scheduler;

    NewsAnalysingInspector() {
        this.prioQueueRef.set(new PriorityBlockingQueue<>());
        this.prioQueueForExchange = new PriorityBlockingQueue<>();
    }

    void start() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "inspector"));
        this.scheduler.scheduleAtFixedRate(this::inspect, 10, 10, TimeUnit.SECONDS);
    }

    void inspect() { // not necessary to synchronize (count and queue) with analysing, results won't differ much
        Util.info("Positive news [last 10s]: %d", this.newsCounter.getAndSet(0));

        final PriorityBlockingQueue<News> queue = this.prioQueueRef.getAndSet(this.prioQueueForExchange);
        final News[] newsArray = toReverseSortedNewsArray(queue);
        Arrays.stream(newsArray).limit(3).forEach(news -> Util.info("Top prio news: %s", news));

        queue.clear();
        this.prioQueueForExchange = queue;
    }

    static News[] toReverseSortedNewsArray(Collection<News> queue) {
        final News[] newsArray = queue.toArray(new News[queue.size()]);
        Arrays.sort(newsArray, Comparator.reverseOrder());
        return newsArray;
    }

    AtomicLong getNewsCounter() {
        return newsCounter;
    }

    AtomicReference<PriorityBlockingQueue<News>> getPrioQueueRef() {
        return prioQueueRef;
    }

    PriorityBlockingQueue<News> getPrioQueueForExchange() {
        return prioQueueForExchange;
    }

    @Override
    public void accept(News news) {
        this.newsCounter.incrementAndGet();
        final PriorityBlockingQueue<News> priorityQueue = this.prioQueueRef.get();
        if (priorityQueue.size() < 3) { // parameterize number of top news
            priorityQueue.offer(news);
        } else {
            if (news.compareTo(priorityQueue.peek()) > 0) {
                priorityQueue.remove();
                priorityQueue.offer(news);
            }
        }
    }

    @Override
    public void close() throws IOException {
        Util.shutdownAndAwaitTermination(this.scheduler, 5);
    }
}
