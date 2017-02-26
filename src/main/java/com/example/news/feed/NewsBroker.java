/*
* Created at 18:06 on 19/02/2017
*/
package com.example.news.feed;

import com.example.news.domain.NewsFramer;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * @author zzhao
 */
class NewsBroker implements Consumer<ByteBuffer> {

    private static final NewsFramer framer = new NewsFramer();

    private final BlockingQueue<byte[]> queue;

    NewsBroker(int queueSize) {
        this.queue = new LinkedBlockingQueue<>(queueSize);
    }

    @Override
    public void accept(ByteBuffer buf) {
        byte[] bytes;
        int len = 0;
        do {
            bytes = this.queue.peek();
            if (bytes != null) {
                len = framer.frameMessage(buf, bytes);
                if (len > 0) {
                    this.queue.remove();
                }
            }
        } while (bytes != null && len > 0);
    }

    void put(byte[] bytes) throws InterruptedException {
        this.queue.put(bytes);
    }
}
