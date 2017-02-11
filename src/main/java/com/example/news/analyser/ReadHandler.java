/*
* Created at 19:37 on 10/02/2017
*/
package com.example.news.analyser;

import com.example.news.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * @author zzhao
 */
public class ReadHandler implements Handler<SelectionKey, IOException> {

    private final Map<SocketChannel, ByteBuffer> bufByChannel;

    private final Analyser analyser;

    public ReadHandler(Map<SocketChannel, ByteBuffer> bufByChannel, Analyser analyser) {
        this.bufByChannel = bufByChannel;
        this.analyser = analyser;
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
            this.bufByChannel.remove(sc);
            Util.debug("<ReadHandler> end %s", sc.getRemoteAddress());
            return;
        }
        if (read > 0) {
            this.analyser.analyse(sc, buf);
        }
        key.interestOps(SelectionKey.OP_READ);
    }
}
