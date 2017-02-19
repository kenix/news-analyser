/*
* Created at 18:06 on 19/02/2017
*/
package com.example.news.analyser;

import com.example.Util;
import com.example.news.domain.News;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsBroker implements Consumer<News> {

    private final BlockingQueue<News> queue;

    NewsBroker() {
        this.queue = new LinkedBlockingQueue<>(1024);
    }

    @Override
    public void accept(News news) {
        if (!this.queue.offer(news)) {
            Util.warn("<NewsBroker> discard %s", news);
        }
    }

    News poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return this.queue.poll(timeout, timeUnit);
    }
}
