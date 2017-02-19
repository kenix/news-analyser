/*
* Created at 22:46 on 14/02/2017
*/
package com.example.nio;

import com.example.Util;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encapsulates non-blocking TCP server implementation using selector.
 * <ul>
 * <li>injectable application protocol</li>
 * <li>single-threaded selector, application protocol</li>
 * <li>configurable period of connection kept alive, otherwise discard</li>
 * </ul>
 *
 * @author zzhao
 */
public class NioTcpServer implements Closeable {

    private static long MILLIS_IN_1_MINUTE = TimeUnit.MINUTES.toMillis(1);

    private final int port;

    private final NioTcpProtocol.Server protocol;

    private final AtomicBoolean shutdown;

    private final CountDownLatch closeLatch;

    private Selector selector;

    public NioTcpServer(int port, NioTcpProtocol.Server protocol) {
        this.port = port;
        this.protocol = protocol;
        this.shutdown = new AtomicBoolean(false);
        this.closeLatch = new CountDownLatch(1);
    }

    public void start() throws IOException {
        try {
            this.selector = Selector.open();

            final ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(this.port));
            ssc.configureBlocking(false);
            ssc.register(this.selector, SelectionKey.OP_ACCEPT);

            this.protocol.start();
            Util.info("<NioTcpServer> listening on %d", this.port);

            while (!this.shutdown.get()) {
                if (this.selector.select(5000) == 0) {
                    continue;
                }

                final Set<SelectionKey> keys = this.selector.selectedKeys();
                for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {
                    final SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) {
                        continue;
                    }

                    updateSelectionTs(key); // update timestamp of client activity

                    if (key.isAcceptable()) {
                        this.protocol.handleAccept(key);
                    }

                    if (key.isReadable()) {
                        this.protocol.handleRead(key);
                    }

                    if (key.isWritable()) {
                        this.protocol.handleWrite(key);
                    }
                }

                cancelInactiveClients(this.selector.keys());
            }
        } finally {
            this.closeLatch.countDown();
        }
    }

    @Override
    public void close() throws IOException {
        if (this.closeLatch.getCount() == 0) { // already in shutdown process
            Util.warn("<NioTcpServer> in closing");
            return;
        }

        this.shutdown.set(true);
        Util.info("<NioTcpServer> closing");
        try {
            this.closeLatch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Util.warn("<NioTcpServer> waiting for end of protocol handling interrupted");
            Thread.currentThread().interrupt();
        }

        closeResource();
        Util.info("<NioTcpServer> closed");
    }

    private void closeResource() {
        Util.close(this.selector);
        Util.close(this.protocol);
    }

    private static void updateSelectionTs(SelectionKey key) {
        if (key.attachment() == null) { // server selector key
            return;
        }
        if (!(key.attachment() instanceof NioContext)) {
            throw new IllegalStateException("attachment of selection key must be of type " +
                    NioContext.class);
        }

        ((NioContext) key.attachment()).setSelectedTs(System.currentTimeMillis());
    }

    private void cancelInactiveClients(Set<SelectionKey> keys) throws IOException {
        final long currentTimeMillis = System.currentTimeMillis();
        for (SelectionKey key : keys) {
            if (key.attachment() == null) { // server selector key
                continue;
            }

            final SocketChannel sc = (SocketChannel) key.channel();
            final NioContext ctx = (NioContext) key.attachment();

            if (currentTimeMillis - ctx.getSelectedTs() >= MILLIS_IN_1_MINUTE) {
                Util.warn("<NioTcpServer> inactive %s", sc.getRemoteAddress());
                closeChannelCancelKey(key, sc);
            } else if (!this.protocol.validateContext(ctx)) {
                Util.debug("<NioTcpServer> bye %s", sc.getRemoteAddress());
                closeChannelCancelKey(key, sc);
            }
        }
    }

    private static void closeChannelCancelKey(SelectionKey key, SocketChannel sc) {
        Util.close(sc);
        key.cancel();
    }
}
