/*
* Created at 23:32 on 11/02/2017
*/
package com.example.news.feed;

import com.example.nio.NioContext;
import com.example.nio.handler.Handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
public class WriteHandler implements Handler<SelectionKey, IOException> {

    private final Consumer<ByteBuffer> consumer;

    public WriteHandler(Consumer<ByteBuffer> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        final ByteBuffer buffer = ((NioContext) key.attachment()).getWriteBuffer();
        this.consumer.accept(buffer);

        buffer.flip();
        final SocketChannel sc = (SocketChannel) key.channel();
        if (buffer.hasRemaining()) {
            sc.write(buffer);
        }
        buffer.compact();
        // for news feed always interested in writing news
    }
}
