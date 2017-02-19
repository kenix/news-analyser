/*
* Created at 18:27 on 19/02/2017
*/
package com.example.nio;

import com.example.Util;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zzhao
 */
public class NioTcpClient implements Closeable {

    private final String serverAddress;

    private final int serverPort;

    private final NioTcpProtocol.Client protocol;

    private SocketChannel socketChannel;

    private Selector selector;

    private final AtomicBoolean shutdown;

    private final CountDownLatch closeLatch;

    public NioTcpClient(String serverAddress, int serverPort, NioTcpProtocol.Client protocol) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.protocol = protocol;
        this.shutdown = new AtomicBoolean(false);
        this.closeLatch = new CountDownLatch(1);
    }

    public void start() throws Exception {
        try {
            this.socketChannel = SocketChannel.open();
            this.socketChannel.configureBlocking(false);
            this.socketChannel.connect(new InetSocketAddress(this.serverAddress, this.serverPort));

            this.selector = Selector.open();
            this.socketChannel.register(this.selector, SelectionKey.OP_CONNECT);

            while (!this.shutdown.get()) {
                if (this.selector.select(2000) == 0) {
                    // non-responsive server, abort
                    throw new TimeoutException(this.serverAddress + ":" + this.serverPort + " timeout");
                }
                final Set<SelectionKey> keys = this.selector.selectedKeys();
                for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {
                    final SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isConnectable()) {
                        this.protocol.handleConnect(key);
                    }

                    if (key.isReadable()) {
                        this.protocol.handleRead(key);
                    }

                    if (key.isWritable()) {
                        this.protocol.handleWrite(key);
                    }
                }
            }
        } finally {
            this.closeLatch.countDown();
        }
    }

    @Override
    public void close() throws IOException {
        Util.close(this.protocol);
        Util.close("NioTcpClient", this.shutdown, this.closeLatch,
                this.selector,
                this.socketChannel
        );
    }
}
