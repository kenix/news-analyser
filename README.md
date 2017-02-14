###Usage
- check out the project: ``git clone https://github.com/kenix/news-analyser.git``
- within project folder: ``./gradlew clean build``
- run mock news analyser with: ``java -cp 'build/libs/*' com.example.news.analyser.NewsReceiver 8080``
- run mock news feed with: ``java -cp 'build/libs/*' com.example.news.feed.NewsUnicaster localhost 8080``
- run mock news feed with news producing rate in milliseconds:
 ``java -cp 'build/libs/*' -DnewsProducingRateInMillis=200 com.example.news.feed.NewsUnicaster localhost 8080``

###Note
- no 3rd-party dependency for runtime except Java SE 8 and this project's classes
- test dependency on groovy and spock
- news receiver is a single threaded selector non-blocking server, could be easily extended using
 executor service. One thread can already handle lots of traffic.
- news analyser is configured with bounded queue and discarding news policy.
- news unicaster has a bounded queue with default size 32. News production will be blocked if this queue is full
 (not being able to be sent off wire). Change this queue size with ``-DnewsQueueSize``
- no specific charset is used for encoding Strings. Encoding scheme of a new's message is open.
 
####To improve
- [ ] algorithm for producing news of higher priority with less probability
- [ ] concurrent priority queue
- [ ] refactor selector code into different server or client implementations supporting application protocols
- [x] refactor analysing and feeding task into their own workers
- [x] other queue and rejection strategies
- [ ] encapsulate feed and analysing code into protocol implementations
- [ ] pool news, bytes or use Unsafe (off-heap) to reduce GC stress 
- [ ] concurrent tests

###Task description
The task involves the development of two separate Java SE 8 programs: a "mock news feed" and a "news analyser". Several instances of the mock news feed will be run simultaneously, each connecting to the same news analyser.
The mock news feed should periodically generate messages containing random news item.
The news analyser should handle the messages from the news feeds and periodically display a short summary about news items that are considered "interesting".

####Mock news feed
Creates a persistent TCP connection to the news analyser and periodically sends news items over that connection. Each news item should be comprised of a headline and a priority.
- The headline of a news item should be a random combination of three to five words from the following list: up, down, rise, fall, good, bad, success, failure, high, low, über, unter.
- The priority of a news item should be an integer within the range [0..9]. News messages with higher priority should be generated with less probability than those with lower priority.
- The frequency of news items being emitted by the feed should be configurable via a Java property.

####News analyser
- Listens on a TCP port for connections from mock news feeds and receives news item messages.
- Inspects the news item headlines and decides whether they are overall positive or negative. If more than 50% of words in the headline are positive ("up", "rise", "good", "success", "high" or "über"), the news item as a whole is considered positive. Negative news items are ignored by the analyser.
- Every 10 seconds, the news analyser should output to the console:
    - the count of positive news items seen during the last 10 seconds
    - the unique headlines of up to three of the highest-priority positive news items seen during the last 10 seconds
