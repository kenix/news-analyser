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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsAnalysingInspector implements Consumer<News>, Closeable {

    private final AtomicReference<InspectorContext> contextRef;

    private InspectorContext contextForExchange;

    private ScheduledExecutorService scheduler;

    private final ReentrantReadWriteLock.ReadLock readLock;

    private final ReentrantReadWriteLock.WriteLock writeLock;

    NewsAnalysingInspector(int numOfTopNewsToKeep) {
        this.contextRef = new AtomicReference<>(new InspectorContext(numOfTopNewsToKeep));
        this.contextForExchange = new InspectorContext(numOfTopNewsToKeep);
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    void start() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "inspector"));
        this.scheduler.scheduleAtFixedRate(this::inspect, 10, 10, TimeUnit.SECONDS);
    }

    void inspect() {
        // TODO find a clever way to avoid this locking: reference counting?
        final InspectorContext ctx = exchangeContext();
        if (ctx == null) {
            Util.error("failed inspecting");
            return;
        }

        Util.info("Positive news last 10s: %d", ctx.getNewsCounter().get());
        final News[] newsArray = toReverseSortedNewsArray(ctx.getPrioQueue());
        Arrays
                .stream(newsArray)
                .limit(ctx.getNumOfTopNewsTopKeep())
                .distinct()
                .forEach(news -> Util.info("Top prio news: %s", news));

        this.contextForExchange = ctx.reset();
    }

    private InspectorContext exchangeContext() {
        try {
            while (!this.writeLock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                Util.warn("delayed inspecting");
            }
            try {
                return this.contextRef.getAndSet(this.contextForExchange);
            } finally {
                this.writeLock.unlock();
            }
        } catch (InterruptedException e) {
            Util.warn("inspector inspecting interrupted %s", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static News[] toReverseSortedNewsArray(Collection<News> queue) {
        final News[] newsArray = queue.toArray(new News[queue.size()]);
        Arrays.sort(newsArray, Comparator.reverseOrder());
        return newsArray;
    }

    @Override
    public void accept(News news) {
        try {
            while (!this.readLock.tryLock(50, TimeUnit.MILLISECONDS)) {
                Util.warn("delayed accepting: %s", news);
            }
            try {
                this.contextRef.get().accept(news);
            } finally {
                this.readLock.unlock();
            }
        } catch (InterruptedException e) {
            Util.warn("inspector accepting interrupted %s", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws IOException {
        Util.shutdownAndAwaitTermination(this.scheduler, 5);
    }

    static class InspectorContext implements Consumer<News> {
        private final AtomicLong newsCounter;
        private final PriorityBlockingQueue<News> prioQueue;
        private final int numOfTopNewsTopKeep;

        InspectorContext(int topNewsToKeep) {
            this.numOfTopNewsTopKeep = topNewsToKeep;
            this.newsCounter = new AtomicLong(0);
            this.prioQueue = new PriorityBlockingQueue<>();
        }

        AtomicLong getNewsCounter() {
            return newsCounter;
        }

        PriorityBlockingQueue<News> getPrioQueue() {
            return prioQueue;
        }

        int getNumOfTopNewsTopKeep() {
            return numOfTopNewsTopKeep;
        }

        @Override
        public void accept(News news) {
            // not necessary to synchronize (count and queue) with analysing, results won't differ much
            this.newsCounter.incrementAndGet();
            // concurrent ops could result in more than 3 elements in the queue and possibly duplicate
            if (this.prioQueue.size() < this.numOfTopNewsTopKeep) { // parameterize number of top news
                this.prioQueue.offer(news);
            } else {
                if (news.compareTo(this.prioQueue.peek()) > 0) {
                    this.prioQueue.remove();
                    this.prioQueue.offer(news);
                }
            }
        }

        InspectorContext reset() {
            this.newsCounter.set(0);
            this.prioQueue.clear();
            return this;
        }
    }
}
