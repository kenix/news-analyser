/*
* Created at 19:22 on 10/02/2017
*/
package com.example.nio.handler;

import com.example.Util;
import com.example.nio.NioContext;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author zzhao
 */
public class AcceptHandler implements Handler<SelectionKey, IOException> {

    private final int bufferSize;

    public AcceptHandler(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        final SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        Util.debug("<AcceptHandler> from %s", sc.getRemoteAddress());

        final SelectionKey clientKey = sc.register(key.selector(), SelectionKey.OP_READ);
        clientKey.attach(new NioContext(this.bufferSize));
    }
}
