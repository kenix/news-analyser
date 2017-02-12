/*
* Created at 19:37 on 10/02/2017
*/
package com.example.handler;

import com.example.news.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
public class ReadHandler implements Handler<SelectionKey, IOException> {

    private final Map<SocketChannel, ByteBuffer> bufByChannel;

    private final Consumer<ByteBuffer> consumer;

    public ReadHandler(Map<SocketChannel, ByteBuffer> bufByChannel, Consumer<ByteBuffer> consumer) {
        this.bufByChannel = bufByChannel;
        this.consumer = consumer;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        final SocketChannel sc = (SocketChannel) key.channel();
        final ByteBuffer buf = this.bufByChannel.get(sc);
        if (buf == null) { // client removed
            return;
        }
        int read = sc.read(buf);
        if (read == -1) { // channel end
            Util.debug("<ReadHandler> end %s", sc.getRemoteAddress());
            this.bufByChannel.remove(sc);
            sc.close();
            key.cancel();
            return;
        }
        if (read > 0) {
            this.consumer.accept(buf);
        }
        key.interestOps(SelectionKey.OP_READ);
    }
}
