/*
* Created at 18:06 on 19/02/2017
*/
package com.example.news.feed;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsBroker implements Consumer<ByteBuffer> {

    private final BlockingQueue<byte[]> queue;

    NewsBroker(int queueSize) {
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    @Override
    public void accept(ByteBuffer buf) {
        byte[] bytes;
        do {
            bytes = this.queue.peek();
            if (bytes != null && buf.remaining() >= bytes.length) {
                buf.put(bytes);
                this.queue.remove();
            }
        } while (bytes != null && buf.remaining() >= bytes.length);
    }

    void put(byte[] bytes) throws InterruptedException {
        this.queue.put(bytes);
    }
}
