/*
* Created at 21:50 on 11/02/2017
*/
package com.example.news.analyser;

import com.example.nio.NioTcpServer;

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

        final NioTcpServer server = new NioTcpServer(Integer.parseInt(args[0]),
                new NewsAnalyser(getIntConfig("numberOfWorkers", 5)));

        // TODO register JMX method to shutdown server cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
            } catch (Exception e) {
                throw new IllegalStateException("error during closing server", e);
            }
        }));

        server.start();
    }
}
