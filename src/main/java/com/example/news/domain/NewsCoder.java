/*
* Created at 18:26 on 25/02/2017
*/
package com.example.news.domain;

import com.example.nio.MessageCoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.example.Util.unsignedToInt;

/**
 * @author zzhao
 */
public class NewsCoder implements MessageCoder<News> {

    /**
     * Encoding schema:
     * <ol>
     * <li>1 byte for priority and headline count: lower 4 bits for priority</li>
     * <li>1 byte for headline count</li>
     * <li>for each headline: 1 byte for headline length plus the headline itself</li>
     * </ol>
     *
     * @param news
     * @return
     */
    @Override
    public byte[] encode(News news) {
        final ByteBuffer buf = ByteBuffer.allocate(encodingLength(news));
        buf.put((byte) (news.getPriority() | (news.getHeadlines().size() << 4)));
        news.getHeadlines().forEach(hl -> {
            buf.put((byte) hl.length()); // 1 byte for headline length
            for (int i = 0; i < hl.length(); i++) {
                final char c = hl.charAt(i);
                if (c > 255) { // maximum of unsigned byte
                    throw new IllegalArgumentException(c + " at pos. " + i + " > 255");
                }
                buf.put((byte) c);
            }
        });

        return buf.array();
    }

    private static int encodingLength(News news) {
        return 1 // 1 byte for priority and headline count
                + news.getHeadlines().size()  // 1 byte for each headline length
                + news.getHeadlines()
                .stream()
                .mapToInt(String::length)
                .sum(); // n bytes for total headline length
    }

    @Override
    public News decode(ByteBuffer buf) {
        final byte firstByte = buf.get();
        final int priority = firstByte & 0x0F;
        final int headlineCount = (firstByte >> 4) & 0x0F;
        return new News(priority, parseHeadlines(buf, headlineCount));
    }

    private static List<String> parseHeadlines(ByteBuffer buf, int headlineCount) {
        final ArrayList<String> headlines = new ArrayList<>(headlineCount);
        for (int i = 0; i < headlineCount; i++) {
            headlines.add(parseHeadLine(buf));
        }
        return headlines;
    }

    private static String parseHeadLine(ByteBuffer buf) {
        final int headlineLength = buf.get();
        final StringBuilder sb = new StringBuilder(headlineLength);
        for (int i = 0; i < headlineLength; i++) {
            sb.append((char) unsignedToInt(buf.get()));
        }
        return sb.toString();
    }
}
