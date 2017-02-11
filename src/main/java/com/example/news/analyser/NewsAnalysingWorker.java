/*
* Created at 22:20 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.news.Util;
import com.example.news.domain.News;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsAnalysingWorker implements Runnable {

    private static final Set<String> HEADLINES_POSITIVE = new HashSet<>(Arrays.asList(
            "up", "rise", "good", "success", "high", "über"
    ));

    private final BlockingQueue<News> queue;

    private final Consumer<News> consumer;

    NewsAnalysingWorker(BlockingQueue<News> queue, Consumer<News> consumer) {
        this.queue = queue;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                final News news = this.queue.poll(1000, TimeUnit.MILLISECONDS);
                if (news != null && isPositiveNews(news)) {
                    this.consumer.accept(news);
                }
            } catch (InterruptedException e) {
                Util.warn("<NewsAnalysingWorker> analysing interrupted %s", e.getMessage());
                Thread.currentThread().interrupt();
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
