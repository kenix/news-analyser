/*
* Created at 18:42 on 25/02/2017
*/
package com.example.news.domain;

import com.example.nio.MessageFramer;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Use one byte to denote message length.
 * <p>
 * Optionally use compression
 * </p>
 *
 * @author zzhao
 */
public class NewsFramer implements MessageFramer {

    @Override
    public int frameMessage(ByteBuffer buffer, byte[] msg) {
        if (buffer.remaining() < msg.length + 1) {
            return 0;
        }
        buffer.put((byte) msg.length).put(msg);
        return msg.length + 1;
    }

    @Override
    public Optional<ByteBuffer> deframeMessage(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return Optional.empty();
        }
        buffer.mark();
        final int len = buffer.get();
        if (buffer.remaining() < len) {
            buffer.reset();
            return Optional.empty();
        }

        final int limit = buffer.limit();
        buffer.limit(buffer.position() + len);
        try {
            return Optional.of(buffer.asReadOnlyBuffer());
        } finally {
            buffer.limit(limit);
            buffer.position(buffer.position() + len);
        }
    }
}
