package com.example.news.domain

import com.example.news.feed.NewsSupplier
import spock.lang.Specification

import java.nio.ByteBuffer

/**
 * @author zzhao
 */
class NewsCoderSpec extends Specification {

    def 'news encoding and parsing'() {
        when: 'create a news coder'
        def coder = new NewsCoder()
        and: 'create a news with priority 3 and all possible headlines'
        def news = new News(3, NewsSupplier.HEADLINES as List)
        and: 'encode this news into this byte buffer'
        def encodedNews = coder.encode(news)
        then: 'the news is correctly encoded'
        encodedNews

        when: 'prepare the byte buffer for decoding news'
        def buf = ByteBuffer.wrap(encodedNews)
        and: 'decode news from this byte buffer'
        def decodedNews = coder.decode(buf)
        then: 'the news is parsed'
        decodedNews
        and: 'has the correct data'
        decodedNews.priority == news.priority
        decodedNews.headlines == news.headlines
    }
}
