/*
* Created at 19:22 on 10/02/2017
*/
package com.example.news.analyser;

import com.example.news.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * @author zzhao
 */
public class AcceptHandler implements Handler<SelectionKey, IOException> {

    private final Map<SocketChannel, ByteBuffer> bufByChannel;

    public AcceptHandler(Map<SocketChannel, ByteBuffer> bufByChannel) {
        this.bufByChannel = bufByChannel;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        final SocketChannel sc = ssc.accept();
        Util.debug("<AcceptHandler> from %s", sc.getRemoteAddress());
        sc.configureBlocking(false);
        this.bufByChannel.put(sc, ByteBuffer.allocate(1024 * 8));
        sc.register(key.selector(), SelectionKey.OP_READ);
    }
}
