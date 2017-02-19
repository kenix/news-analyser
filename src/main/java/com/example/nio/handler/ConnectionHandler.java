/*
* Created at 21:35 on 19/02/2017
*/
package com.example.nio.handler;

import com.example.Util;
import com.example.nio.NioContext;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author zzhao
 */
public class ConnectionHandler implements Handler<SelectionKey, IOException> {

    private final Supplier<NioContext> supplier;

    private final Consumer<SocketAddress> consumer;

    public ConnectionHandler(Supplier<NioContext> supplier, Consumer<SocketAddress> consumer) {
        this.supplier = supplier;
        this.consumer = consumer;
    }

    @Override
    public void handle(SelectionKey key) throws IOException {
        final SocketChannel sc = (SocketChannel) key.channel();
        if (sc.finishConnect()) {
            Util.info("<ConnectionHandler> connected to %s", sc.getRemoteAddress());
            this.consumer.accept(sc.getLocalAddress());
            final NioContext nioContext = this.supplier.get();
            key.interestOps(nioContext.getInterestOps());
            key.attach(nioContext);
        }
    }
}
