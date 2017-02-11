/*
* Created at 20:11 on 11/02/2017
*/
package com.example.news.feed;

import com.example.news.Util;
import com.example.news.domain.News;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

/**
 * @author zzhao
 */
class NewsProducer implements Runnable {

    private final BlockingQueue<byte[]> queue;
    private final Supplier<News> supplier;
    private final ByteBuffer buffer;

    NewsProducer(BlockingQueue<byte[]> queue, Supplier<News> supplier) {
        this.queue = queue;
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
            this.queue.put(Arrays.copyOfRange(this.buffer.array(), 0, this.buffer.limit()));
        } catch (InterruptedException e) {
            Util.warn("producing news interrupted, %s", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            this.buffer.clear();
        }
    }
}
