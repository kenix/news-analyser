/*
* Created at 21:50 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.nio.NioTcpServer;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.Util.getIntConfig;

/**
 * @author zzhao
 */
public class NewsReceiver {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: <port>");
            System.exit(1);
        }

        final int port = Integer.parseInt(args[0]);
        try (
                final NioTcpServer server = new NioTcpServer(port,
                        new NewsAnalyser(getIntConfig("numberOfWorkers", 5)),
                        new AtomicBoolean(false)) // TODO register JMX method to shutdown server cleanly
        ) {
            server.start();
        }
    }
}
