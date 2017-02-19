/*
* Created at 22:11 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.Util;
import com.example.news.domain.News;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsAssembler implements Consumer<ByteBuffer> {

    private final Consumer<News> consumer;

    NewsAssembler(Consumer<News> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(ByteBuffer byteBuffer) {
        byteBuffer.flip();
        Optional<News> news;
        do {
            news = Util.parseNews(byteBuffer);
            news.ifPresent(this.consumer);
        } while (news.isPresent());
        byteBuffer.compact();
    }
}
