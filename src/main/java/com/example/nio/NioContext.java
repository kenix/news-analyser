/*
* Created at 16:16 on 19/02/2017
*/
package com.example.nio;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * @author zzhao
 */
public class NioContext {

    private final ByteBuffer readBuffer;

    private final ByteBuffer writeBuffer;

    private long selectedTs;

    private boolean readStreamReached;

    private boolean writeStreamReached;

    public NioContext(int readBufferSize, int writeBufferSize) {
        this.selectedTs = System.currentTimeMillis();
        this.readBuffer = readBufferSize > 0
                ? ByteBuffer.allocateDirect(readBufferSize)
                : null;
        this.writeBuffer = writeBufferSize > 0
                ? ByteBuffer.allocateDirect(writeBufferSize)
                : null;
    }

    public static NioContext onlyRead(int bufferSize) {
        return new NioContext(bufferSize, 0);
    }

    public static NioContext onlyWrite(int bufferSize) {
        return new NioContext(0, bufferSize);
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public long getSelectedTs() {
        return selectedTs;
    }

    public void setSelectedTs(long selectedTs) {
        this.selectedTs = selectedTs;
    }

    public boolean isReadStreamEnded() {
        return readStreamReached;
    }

    public void endReadStream() {
        this.readStreamReached = true;
    }

    public boolean isWriteStreamReached() {
        return writeStreamReached;
    }

    public void endWriteStream() {
        this.writeStreamReached = true;
    }

    public int getInterestOps() {
        int ops = 0;

        if (this.readBuffer != null) {
            ops |= SelectionKey.OP_READ;
        }

        if (this.writeBuffer != null) {
            ops |= SelectionKey.OP_WRITE;
        }

        return ops;
    }
}
