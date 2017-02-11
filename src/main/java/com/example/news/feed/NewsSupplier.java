/*
* Created at 20:17 on 11/02/2017
*/
package com.example.news.feed;

import com.example.news.domain.News;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * @author zzhao
 */
public class NewsSupplier implements Supplier<News> {

    public static final String[] HEADLINES = new String[]{
            "up", "down", "rise", "fall", "good", "bad", "success", "failure", "high", "low", "über", "unter"
    };

    private final SecureRandom random;

    NewsSupplier() {
        this.random = new SecureRandom(String.valueOf(System.nanoTime()).getBytes());
    }

    @Override
    public News get() {
        final int oneFrom100 = this.random.nextInt(100);
        final int priority = oneFrom100 < 7 // 7% chance to be high prio
                ? oneFrom100 % 5 + 5
                : oneFrom100 % 5;

        final int headlineCount = this.random.nextInt(3) + 3; // 3 to 5
        final ArrayList<String> headlines = new ArrayList<>(headlineCount);
        for (int i = 0; i < headlineCount; i++) {
            headlines.add(HEADLINES[this.random.nextInt(HEADLINES.length)]); // could have duplicate headlines
        }

        return new News(priority, headlines);
    }
}
