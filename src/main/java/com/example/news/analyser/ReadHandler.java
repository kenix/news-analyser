/*
* Created at 19:37 on 10/02/2017
*/
package com.example.news.analyser;

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
public class ReadHandler implements Handler<SelectionKey, IOException> {

    private final Consumer<ByteBuffer> consumer;

    public ReadHandler(Consumer<ByteBuffer> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        final SocketChannel sc = (SocketChannel) key.channel();
        final NioContext nioContext = (NioContext) key.attachment();

        final ByteBuffer buf = nioContext.getReadBuffer();
        int read = sc.read(buf);
        if (read == -1) { // channel end
            nioContext.endReadStream();
            return;
        }

        if (read > 0) {
            this.consumer.accept(buf);
        }

        key.interestOps(SelectionKey.OP_READ);
    }
}
