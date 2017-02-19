/*
* Created at 16:16 on 19/02/2017
*/
package com.example.nio;

import java.nio.ByteBuffer;

/**
 * @author zzhao
 */
public class NioContext {

    private final ByteBuffer buffer;

    private long selectedTs;

    private boolean readStreamReached;

    private boolean writeStreamReached;

    public NioContext(int bufferSize) {
        this.selectedTs = System.currentTimeMillis();
        this.buffer = ByteBuffer.allocateDirect(bufferSize);
    }

    public ByteBuffer getBuffer() {
        return buffer;
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
}
