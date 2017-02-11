/*
* Created at 21:50 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.news.Util;
import com.example.news.domain.News;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
public class NewsAnalyser implements Closeable, Consumer<News> {

    private final AtomicInteger analyserThreadCount = new AtomicInteger(0);

    private final int numberOfWorkers;

    private final BlockingQueue<News> queue;

    private final NewsAnalysingInspector inspector;

    private ExecutorService executorService;

    public NewsAnalyser(int numberOfWorkers) {
        this.numberOfWorkers = numberOfWorkers;
        this.queue = new LinkedBlockingQueue<>(1024);
        this.inspector = new NewsAnalysingInspector();
    }

    void start() {
        this.executorService = Executors.newFixedThreadPool(this.numberOfWorkers,
                r -> new Thread(r, "analyser-" + this.analyserThreadCount.incrementAndGet()));
        for (int i = 0; i < this.numberOfWorkers; i++) {
            this.executorService.execute(new NewsAnalysingWorker(this.queue, this.inspector));
        }
        this.inspector.start();
    }

    @Override
    public void close() throws IOException {
        Util.shutdownAndAwaitTermination(this.executorService, 5);
        this.inspector.close();
    }

    @Override
    public void accept(News news) {
        if (!this.queue.offer(news)) {
            Util.warn("discard %s", news);
        }
    }
}
