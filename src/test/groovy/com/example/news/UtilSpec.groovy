package com.example.news

import com.example.news.domain.News
import com.example.news.feed.Feed
import spock.lang.Specification

import java.nio.ByteBuffer

/**
 * @author zzhao
 */
class UtilSpec extends Specification {

    def 'byte to unsigned int'() {
        expect: 'correct conversion between unsigned byte and int'
        Util.unsignedToInt(0 as byte) == 0 as int
        Util.unsignedToInt(1 as byte) == 1 as int
        Util.unsignedToInt(-1 as byte) == 255 as int
        Util.unsignedToInt(255 as byte) == 255 as int
        Util.unsignedToInt(127 as byte) == 127 as int
        Util.unsignedToInt(130 as byte) == 130 as int
    }

    def 'news encoding and parsing'() {
        when: 'creating a news with priority 3 and all possible headlines'
        def news = new News(3, Feed.HEADLINES as List)
        and: 'byte buffer with size of 255'
        def buf = ByteBuffer.allocate(255)
        and: 'encoding this news into this byte buffer'
        def len = Util.encodeNews(buf, news)
        then: 'the news is correctly encoded'
        len
        len == Util.encodingLength(news)

        when: 'preparing the byte buffer for parsing news'
        buf.flip()
        and: 'parsing news from this byte buffer'
        def parsedNews = Util.parseNews(buf)
        then: 'the news is parsed'
        parsedNews.present
        and: 'has the correct data'
        parsedNews.get().priority == news.priority
        parsedNews.get().headlines == news.headlines
    }
}
