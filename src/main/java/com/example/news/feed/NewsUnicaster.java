/*
* Created at 21:04 on 11/02/2017
*/
package com.example.news.feed;

import com.example.news.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * @author zzhao
 */
public class NewsUnicaster {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: <peer host name> <port>");
            System.exit(1);
        }
        final String hostName = args[0];
        final int port = Integer.parseInt(args[1]);

        final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(getIntConfig("newsQueueSize", 32));
        try (
                final Selector selector = Selector.open();
                final NewsFeed feed = new NewsFeed(new NewsProducer(queue, new NewsSupplier()),
                        getIntConfig("newsProducingRateInMillis", 1000))
        ) {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            if (!sc.connect(new InetSocketAddress(hostName, port))) {
                sc.register(selector, SelectionKey.OP_CONNECT);
                if (selector.select(2000) == 0) {
                    throw new TimeoutException("cannot connect to " + hostName + ":" + port + " in 2 seconds");
                }
                final SelectionKey key = getTheSelectionKey(selector);
                if (key.isConnectable()) {
                    sc = (SocketChannel) key.channel();
                    if (sc.finishConnect()) {
                        Util.info("<NewsUnicaster> connected to %s", sc.getRemoteAddress());
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                } else {
                    throw new IllegalStateException("expected OP_CONNECT");
                }
            } else {
                sc.register(selector, SelectionKey.OP_WRITE);
            }

            feed.start(sc.getLocalAddress().toString());
            final ByteBuffer buf = ByteBuffer.allocateDirect(Util.DEFAULT_BUF_LENGTH);

            while (true) {
                if (selector.select(2000) == 0) {
                    Util.info("<NewsUnicaster> -");
                    continue; // continue or end - non-responsive server
                }
                final SelectionKey key = getTheSelectionKey(selector);
                if (key.isWritable()) {
                    sendNews(key, buf, queue);
                }
            }
        }
    }

    private static void sendNews(SelectionKey key, ByteBuffer buf, Queue<byte[]> newsPool) throws IOException {
        takeNews(buf, newsPool);
        wireNews(key, buf);

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private static void wireNews(SelectionKey key, ByteBuffer buf) throws IOException {
        buf.flip();
        final SocketChannel sc = (SocketChannel) key.channel();
        while (buf.hasRemaining()) {
            sc.write(buf);
        }
        buf.clear();
    }

    private static void takeNews(ByteBuffer buf, Queue<byte[]> newsPool) {
        byte[] bytes;
        do {
            bytes = newsPool.peek();
            if (bytes != null && buf.remaining() >= bytes.length) {
                buf.put(bytes);
                newsPool.remove();
            }
        } while (bytes != null && buf.remaining() >= bytes.length);
    }

    private static SelectionKey getTheSelectionKey(Selector selector) {
        final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        final SelectionKey key = it.next();
        it.remove();
        return key;
    }

    private static int getIntConfig(String name, int defaultVal) {
        return Integer.parseInt(System.getProperty(name, String.valueOf(defaultVal)));
    }
}
