/*
* Created at 21:08 on 11/02/2017
*/
package com.example.news.feed;

import com.example.news.Util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zzhao
 */
class NewsFeed implements Closeable {

    private final int newsProducingRateInMillis;

    private final NewsProducer newsProducer;

    private ScheduledExecutorService scheduler;

    NewsFeed(NewsProducer newsProducer, int newsProducingRateInMillis) {
        this.newsProducer = newsProducer;
        this.newsProducingRateInMillis = newsProducingRateInMillis;
    }

    void start(String feedAddr) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "f" + feedAddr));
        this.scheduler.scheduleAtFixedRate(this.newsProducer, 100,
                this.newsProducingRateInMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        Util.shutdownAndAwaitTermination(this.scheduler, 5);
    }
}
