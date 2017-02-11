/*
* Created at 21:50 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.handler.AcceptHandler;
import com.example.handler.ExceptionHandler;
import com.example.handler.Handler;
import com.example.handler.ReadHandler;
import com.example.news.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.example.news.Util.getIntConfig;

/**
 * @author zzhao
 */
public class NewsReceiver {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: <port>");
            System.exit(1);
        }
        final int port = Integer.parseInt(args[0]);
        try (
                final Selector selector = Selector.open();
                final NewsAnalyser analyser = new NewsAnalyser(getIntConfig("numberOfWorkers", 5))
        ) {
            final Map<SocketChannel, ByteBuffer> bufByChan = new HashMap<>(); // single thread receiver
            final Handler<SelectionKey, IOException> acceptHandler =
                    new ExceptionHandler<>(new AcceptHandler(bufByChan));
            final Handler<SelectionKey, IOException> readHandler =
                    new ExceptionHandler<>(new ReadHandler(bufByChan, new NewsAssembler(analyser)));

            final ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(port));
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            analyser.start();
            Util.info("<NewsReceiver> ready for connections on %d", port);

            while (true) {
                selector.select();
                final Set<SelectionKey> keys = selector.selectedKeys();
                for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            acceptHandler.handle(key);
                        } else if (key.isReadable()) {
                            readHandler.handle(key);
                        } // not interested in other ops
                    }
                }
            }
        }
    }
}
