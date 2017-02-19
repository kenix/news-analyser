/*
* Created at 20:11 on 11/02/2017
*/
package com.example.news.feed;

import com.example.Util;
import com.example.news.domain.News;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * @author zzhao
 */
class NewsProducer implements Runnable {

    private final NewsBroker newsBroker;

    private final Supplier<News> supplier;

    private final ByteBuffer buffer;

    NewsProducer(NewsBroker newsBroker, Supplier<News> supplier) {
        this.newsBroker = newsBroker;
        this.supplier = supplier;
        this.buffer = ByteBuffer.allocate(Util.MAX_NEWS_LENGTH);
    }

    @Override
    public void run() {
        final News news = this.supplier.get();
        Util.debug("produced %s", news);
        Util.encodeNews(this.buffer, news);
        this.buffer.flip();
        try {
            // blocking if queue is full
            this.newsBroker.put(Arrays.copyOfRange(this.buffer.array(), 0, this.buffer.limit()));
        } catch (InterruptedException e) {
            Util.warn("producing news interrupted");
            Thread.currentThread().interrupt();
        } finally {
            this.buffer.clear();
        }
    }
}
