/*
* Created at 18:18 on 25/02/2017
*/
package com.example.nio;

import java.nio.ByteBuffer;

/**
 * @author zzhao
 */
public interface MessageCoder<T> {

    byte[] encode(T msg);

    T decode(ByteBuffer buf);
}
