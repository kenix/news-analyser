/*
* Created at 23:32 on 11/02/2017
*/
package com.example.nio.handler;

import com.example.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
public class WriteHandler implements Handler<SelectionKey, IOException> {

    private final ByteBuffer buf = ByteBuffer.allocateDirect(Util.DEFAULT_BUF_LENGTH);

    private final Consumer<ByteBuffer> consumer;

    public WriteHandler(Consumer<ByteBuffer> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        this.consumer.accept(this.buf);
        this.buf.flip();
        final SocketChannel sc = (SocketChannel) key.channel();
        if (this.buf.hasRemaining()) {
            sc.write(this.buf);
        }
        this.buf.compact();

        key.interestOps(SelectionKey.OP_WRITE);
    }
}
