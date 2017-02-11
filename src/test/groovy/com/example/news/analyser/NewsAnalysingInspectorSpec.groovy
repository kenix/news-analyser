package com.example.news.analyser

import com.example.news.domain.News
import spock.lang.Specification

import java.util.concurrent.PriorityBlockingQueue

/**
 * @author zzhao
 */
class NewsAnalysingInspectorSpec extends Specification {

    def 'sort news'() {
        given: 'a priority queue'
        def prioQueue = new PriorityBlockingQueue<News>()

        when: 'putting 5 news into the queue'
        prioQueue.offer(new News(1, ['down', 'down', 'down']))
        prioQueue.offer(new News(2, ['up', 'down', 'down']))
        prioQueue.offer(new News(3, ['up', 'up', 'down']))
        prioQueue.offer(new News(4, ['up', 'up', 'up']))
        prioQueue.offer(new News(5, ['up', 'up', 'down', 'down']))
        and: 'letting the news analysing inspector sort those news in the queue'
        def sortedNews = NewsAnalysingInspector.toReverseSortedNewsArray(prioQueue)
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
        given: 'a news analysing inspector'
        def inspector = new NewsAnalysingInspector()

        when: 'accepting 5 news'
        inspector.accept(new News(1, ['down', 'down', 'down']))
        inspector.accept(new News(2, ['up', 'down', 'down']))
        inspector.accept(new News(3, ['up', 'up', 'down']))
        inspector.accept(new News(4, ['up', 'up', 'up']))
        inspector.accept(new News(5, ['up', 'up', 'down', 'down']))
        then: 'the analyser counted the news correctly'
        inspector.newsCounter.get() == 5L
        and: 'only retained top 3 news'
        inspector.prioQueueRef.get().size() == 3
        and: 'the prio queue for exchange between inspecting and analysing threads is empty'
        !inspector.prioQueueForExchange.size()

        when: 'the analyser inspects the statistics'
        inspector.inspect()
        then: "the analyser's counter for news is reset"
        !inspector.newsCounter.get()
        and: 'the prio queue is empty after inspecting without parallel analysing'
        !inspector.prioQueueRef.get().size()
        and: 'the prio queue for exchange between inspecting and analysing threads is reset'
        !inspector.prioQueueForExchange.size()
    }
}
