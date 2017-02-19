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
import java.util.function.Supplier;

/**
 * @author zzhao
 */
public class AcceptHandler implements Handler<SelectionKey, IOException> {

    private final Supplier<NioContext> supplier;

    public AcceptHandler(Supplier<NioContext> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        final ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        final SocketChannel sc = ssc.accept();
        sc.configureBlocking(false);
        Util.debug("<AcceptHandler> from %s", sc.getRemoteAddress());

        final NioContext nioContext = this.supplier.get();
        final SelectionKey clientKey = sc.register(key.selector(), nioContext.getInterestOps());
        clientKey.attach(nioContext);
    }
}
