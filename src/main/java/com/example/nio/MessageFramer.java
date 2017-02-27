/*
* Created at 18:18 on 25/02/2017
*/
package com.example.nio;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Frames message to and from wire, can optionally employ encryption a/o compression.
 *
 * @author zzhao
 */
public interface MessageFramer {

    /**
     * Frames the given message into the given buffer if possible.
     *
     * @param buffer
     * @param msg
     * @return the total bytes used for framing the given message. 0 if the given message cannot be framed into
     * the given buffer.
     */
    int toWire(ByteBuffer buffer, byte[] msg);

    /**
     * Frames message from wire if possible.
     *
     * @param buffer
     * @return
     */
    Optional<ByteBuffer> fromWire(ByteBuffer buffer);
}
