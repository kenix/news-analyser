/*
* Created at 22:20 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.Util;
import com.example.news.domain.News;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsAnalysingWorker implements Runnable {

    private static final Set<String> HEADLINES_POSITIVE = new HashSet<>(Arrays.asList(
            "up", "rise", "good", "success", "high", "über"
    ));

    private final NewsBroker newsBroker;

    private final Consumer<News> consumer;

    NewsAnalysingWorker(NewsBroker newsBroker, Consumer<News> consumer) {
        this.newsBroker = newsBroker;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final News news = this.newsBroker.poll(1000, TimeUnit.MILLISECONDS);
                if (news != null && isPositiveNews(news)) {
                    this.consumer.accept(news);
                }
            } catch (InterruptedException e) {
                Util.warn("<NewsAnalysingWorker> analysing interrupted");
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
