/*
* Created at 22:11 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.news.domain.News;
import com.example.news.domain.NewsCoder;
import com.example.news.domain.NewsFramer;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsAssembler implements Consumer<ByteBuffer> {

    private static final NewsFramer framer = new NewsFramer();

    private static final NewsCoder coder = new NewsCoder();

    private final Consumer<News> consumer;

    NewsAssembler(Consumer<News> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(ByteBuffer byteBuffer) {
        byteBuffer.flip();
        Optional<ByteBuffer> data;
        do {
            data = framer.deframeMessage(byteBuffer);
            data.map(coder::decode).ifPresent(this.consumer);
        } while (data.isPresent());
        byteBuffer.compact();
    }
}
