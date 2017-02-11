package com.example.news.analyser

import com.example.news.domain.News
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.PriorityBlockingQueue

/**
 * @author zzhao
 */
class AnalyserSpec extends Specification {

    @Unroll
    def '#news is positive #status'() {
        given:
        def analyser = new Analyser()

        expect:
        analyser.isPositiveNews(news) == status

        where:
        news                                      | status
        new News(2, ['down', 'down', 'down'])     | false
        new News(2, ['up', 'down', 'down'])       | false
        new News(2, ['up', 'up', 'down', 'down']) | false
        new News(2, ['up', 'up', 'down'])         | true
        new News(2, ['up', 'up', 'up'])           | true
    }

    def 'sort news'() {
        given:
        def analyser = new Analyser()
        and:
        def prioQueue = new PriorityBlockingQueue<News>()
        and:
        prioQueue.offer(new News(1, ['down', 'down', 'down']))
        prioQueue.offer(new News(2, ['up', 'down', 'down']))
        prioQueue.offer(new News(3, ['up', 'up', 'down']))
        prioQueue.offer(new News(4, ['up', 'up', 'up']))
        prioQueue.offer(new News(5, ['up', 'up', 'down', 'down']))

        when:
        def sortedNews = analyser.sortNews(prioQueue)
        then:
        sortedNews.length == 5
        sortedNews as List == [
                new News(5, ['up', 'up', 'down', 'down']),
                new News(4, ['up', 'up', 'up']),
                new News(3, ['up', 'up', 'down']),
                new News(2, ['up', 'down', 'down']),
                new News(1, ['down', 'down', 'down']),
        ]
    }

    def 'inspect news'() {
        given:
        def analyser = new Analyser()

        when:
        analyser.analyseNews(new News(1, ['down', 'down', 'down']))
        analyser.analyseNews(new News(2, ['up', 'down', 'down']))
        analyser.analyseNews(new News(3, ['up', 'up', 'down']))
        analyser.analyseNews(new News(4, ['up', 'up', 'up']))
        analyser.analyseNews(new News(5, ['up', 'up', 'down', 'down']))
        then:
        analyser.positiveNewsCount.get() == 2L
        and:
        analyser.prioQueueRef.get().size() == 2
        and:
        !analyser.prioQueueForExchange.size()

        when:
        analyser.inspect()
        then:
        !analyser.positiveNewsCount.get()
        and:
        !analyser.prioQueueRef.get().size()
        and:
        !analyser.prioQueueForExchange.size()
    }
}
