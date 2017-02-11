/*
* Created at 19:10 on 10/02/2017
*/
package com.example.news.feed;

import com.example.news.Util;
import com.example.news.domain.News;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * @author zzhao
 */
public class Feed implements Closeable {

    public static final String[] HEADLINES = new String[]{
            "up", "down", "rise", "fall", "good", "bad", "success", "failure", "high", "low", "über", "unter"
    };

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: <analyser host name> <port>");
            System.exit(1);
        }
        final String hostName = args[0];
        final int port = Integer.parseInt(args[1]);

        final Queue<byte[]> newsPool = new LinkedBlockingQueue<>(getIntConfig("newsQueueSize", 32));
        try (
                final Selector selector = Selector.open();
                final Feed feed = new Feed(newsPool, getIntConfig("newsPopRateInMillis", 1000))
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
                        Util.info("<Feed.main> connected to %s", sc.getRemoteAddress());
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
                    Util.info("<Feed.main> -");
                    continue; // continue or end - non-responsive server
                }
                final SelectionKey key = getTheSelectionKey(selector);
                if (key.isWritable()) {
                    sendNews(key, buf, newsPool);
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

    private final SecureRandom random = new SecureRandom(String.valueOf(System.nanoTime()).getBytes());

    private final ByteBuffer buf = ByteBuffer.allocate(Util.MAX_NEWS_LENGTH);

    private final Queue<byte[]> newsPool;

    private final int newsPopRateInMillis;

    private ScheduledExecutorService scheduler;

    private Feed(Queue<byte[]> newsPool, int newsPopRateInMillis) {
        this.newsPool = newsPool;
        this.newsPopRateInMillis = newsPopRateInMillis;
    }

    private void start(String addr) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "f" + addr));
        this.scheduler.scheduleAtFixedRate(() -> {
            final News news = popNews();
            Util.debug("<Feed.start> %s", news);
            Util.encodeNews(this.buf, news);
            this.buf.flip();
            if (!this.newsPool.offer(Arrays.copyOfRange(this.buf.array(), 0, this.buf.limit()))) {
                Util.warn("<Feed.start> drop {}", news);
            }
            this.buf.clear();
        }, 100, this.newsPopRateInMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws IOException {
        Util.shutdownAndAwaitTermination(this.scheduler, 5);
    }

    private News popNews() {
        int priority = this.random.nextInt(5);
        if (this.random.nextInt(100) < 7) { // 7% chance to be high prio
            priority += 5;
        }

        final int headlineCount = this.random.nextInt(3) + 3; // 3 to 5
        final ArrayList<String> headlines = new ArrayList<>(headlineCount);
        for (int i = 0; i < headlineCount; i++) {
            headlines.add(HEADLINES[this.random.nextInt(HEADLINES.length)]); // could have duplicate headlines
        }

        return new News(priority, headlines);
    }
}
