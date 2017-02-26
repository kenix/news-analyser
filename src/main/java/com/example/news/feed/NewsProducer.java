/*
* Created at 20:11 on 11/02/2017
*/
package com.example.news.feed;

import com.example.Util;
import com.example.news.domain.News;
import com.example.news.domain.NewsCoder;

import java.util.function.Supplier;

/**
 * @author zzhao
 */
class NewsProducer implements Runnable {

    private final static NewsCoder coder = new NewsCoder();

    private final NewsBroker newsBroker;

    private final Supplier<News> supplier;

    NewsProducer(NewsBroker newsBroker, Supplier<News> supplier) {
        this.newsBroker = newsBroker;
        this.supplier = supplier;
    }

    @Override
    public void run() {
        final News news = this.supplier.get();
        Util.debug("produced %s", news);
        final byte[] encodedNews = coder.encode(news);
        try {
            // blocking if queue is full
            this.newsBroker.put(encodedNews);
        } catch (InterruptedException e) {
            Util.warn("producing news interrupted");
            Thread.currentThread().interrupt();
        }
    }
}
