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
    def '#news positivity #status'() {
        given: 'an analyser'
        def analyser = new Analyser()

        expect: 'the analyser can decide if a news if positive'
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
        given: 'an analyser'
        def analyser = new Analyser()
        and: 'a priority queue'
        def prioQueue = new PriorityBlockingQueue<News>()

        when: 'putting 5 news into the queue'
        prioQueue.offer(new News(1, ['down', 'down', 'down']))
        prioQueue.offer(new News(2, ['up', 'down', 'down']))
        prioQueue.offer(new News(3, ['up', 'up', 'down']))
        prioQueue.offer(new News(4, ['up', 'up', 'up']))
        prioQueue.offer(new News(5, ['up', 'up', 'down', 'down']))
        and: 'letting the analyser to sort those news in the queue'
        def sortedNews = analyser.sortNews(prioQueue)
        then: 'getting 5 news back'
        sortedNews.length == 5
        and: 'they are in the expected order: prio desc'
        sortedNews as List == [
                new News(5, ['up', 'up', 'down', 'down']),
                new News(4, ['up', 'up', 'up']),
                new News(3, ['up', 'up', 'down']),
                new News(2, ['up', 'down', 'down']),
                new News(1, ['down', 'down', 'down']),
        ]
    }

    def 'inspect news'() {
        given: 'an analyser'
        def analyser = new Analyser()

        when: 'analysing 5 news, of which 2 are positive'
        analyser.analyseNews(new News(1, ['down', 'down', 'down']))
        analyser.analyseNews(new News(2, ['up', 'down', 'down']))
        analyser.analyseNews(new News(3, ['up', 'up', 'down']))
        analyser.analyseNews(new News(4, ['up', 'up', 'up']))
        analyser.analyseNews(new News(5, ['up', 'up', 'down', 'down']))
        then: 'the analyser counted the positive news correctly'
        analyser.positiveNewsCount.get() == 2L
        and: 'ignored negative news, i.e. only queued positive news'
        analyser.prioQueueRef.get().size() == 2
        and: 'the prio queue for exchange between inspecting and analysing threads is empty'
        !analyser.prioQueueForExchange.size()

        when: 'the analyser inspects the statistics'
        analyser.inspect()
        then: "the analyser's counter for positive news is reset"
        !analyser.positiveNewsCount.get()
        and: 'the prio queue is empty after inspecting without parallel analysing'
        !analyser.prioQueueRef.get().size()
        and: 'the prio queue for exchange between inspecting and analysing threads is reset'
        !analyser.prioQueueForExchange.size()
    }
}
