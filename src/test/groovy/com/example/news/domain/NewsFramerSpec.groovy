package com.example.news.domain

import spock.lang.Specification

import java.nio.ByteBuffer

/**
 * @author zzhao
 */
class NewsFramerSpec extends Specification {

    def 'can frame and deframe message'() {
        when:
        def framer = new NewsFramer()
        and:
        def buf = ByteBuffer.allocate(16)
        and:
        def messages = [
                [0b1, 0b0, 0b1, 0b1, 0b0] as byte[],
                [0b1, 0b0, 0b1, 0b1, 0b1] as byte[],
                [0b1, 0b1, 0b0, 0b0, 0b0] as byte[],
                [0b1, 0b1, 0b0, 0b0, 0b1] as byte[],
        ]
        then:
        framer.frameMessage(buf, messages[0])
        framer.frameMessage(buf, messages[1])
        !framer.frameMessage(buf, messages[2])

        when:
        buf.flip()
        and:
        def msg = framer.deframeMessage(buf)
        then:
        msg.isPresent()
        bytesEqual(messages[0], msg.get())

        when:
        msg = framer.deframeMessage(buf)
        then:
        msg.isPresent()
        bytesEqual(messages[1], msg.get())

        when:
        msg = framer.deframeMessage(buf)
        then:
        !msg.isPresent()

        when:
        buf.compact()
        then:
        framer.frameMessage(buf, messages[2])
        framer.frameMessage(buf, messages[3])

        when:
        buf.flip()
        msg = framer.deframeMessage(buf)
        then:
        msg.isPresent()
        bytesEqual(messages[2], msg.get())

        when:
        msg = framer.deframeMessage(buf)
        then:
        msg.isPresent()
        bytesEqual(messages[3], msg.get())

        when:
        msg = framer.deframeMessage(buf)
        then:
        !msg.isPresent()
    }

    private static void bytesEqual(byte[] bytes, ByteBuffer buf) {
        for (int i = 0; i < bytes.length; i++) {
            assert bytes[i] == buf.get()
        }
    }
}
