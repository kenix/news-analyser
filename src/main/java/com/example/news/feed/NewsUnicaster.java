/*
* Created at 21:04 on 11/02/2017
*/
package com.example.news.feed;

import com.example.nio.NioTcpClient;

import static com.example.Util.getIntConfig;

/**
 * @author zzhao
 */
public class NewsUnicaster {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: <server address> <server port>");
            System.exit(1);
        }

        try (
                final NioTcpClient client = new NioTcpClient(args[0], Integer.parseInt(args[1]),
                        new NewsFeed(getIntConfig("newsProducingRateInMillis", 1000)))
        ) {
            client.start();
        }
    }
}
