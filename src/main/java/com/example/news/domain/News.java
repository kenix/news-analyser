/*
* Created at 20:24 on 10/02/2017
*/
package com.example.news.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author zzhao
 */
public class News implements Comparable<News> {

    private final int priority;

    private final List<String> headlines;

    // to reduce size just keep the sorted indices
    private final List<String> sortedHeadlines;

    public News(int priority, List<String> headlines) {
        this.priority = priority;
        this.headlines = headlines;
        this.sortedHeadlines = new ArrayList<>(headlines);
        Collections.sort(this.sortedHeadlines);
    }

    public int getPriority() {
        return priority;
    }

    public List<String> getHeadlines() {
        return headlines;
    }

    @Override
    public String toString() {
        return "News{" + priority + "," + headlines + '}';
    }

    @Override
    public int compareTo(News that) {
        int result = Integer.compare(this.priority, that.priority);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(this.headlines.size(), that.headlines.size());
        if (result != 0) {
            return result;
        }
        for (int i = 0; i < this.sortedHeadlines.size(); i++) {
            result = this.sortedHeadlines.get(i).compareTo(that.sortedHeadlines.get(i));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
}
