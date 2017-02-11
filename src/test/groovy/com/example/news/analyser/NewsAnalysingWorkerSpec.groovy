package com.example.news.analyser

import com.example.news.domain.News
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author zzhao
 */
class NewsAnalysingWorkerSpec extends Specification {

    @Unroll
    def '#news positivity #status'() {
        given: 'a news analysing worker'
        def newsAnalysingWorker = new NewsAnalysingWorker(null, null)

        expect: 'the analyser can decide if a news if positive'
        newsAnalysingWorker.isPositiveNews(news) == status

        where:
        news                                      | status
        new News(2, ['down', 'down', 'down'])     | false
        new News(2, ['up', 'down', 'down'])       | false
        new News(2, ['up', 'up', 'down', 'down']) | false
        new News(2, ['up', 'up', 'down'])         | true
        new News(2, ['up', 'up', 'up'])           | true
    }
}
