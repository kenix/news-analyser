/*
* Created at 04:24 on 11/02/2017
*/
package com.example.handler;

import com.example.news.Util;

import java.util.function.BiConsumer;

/**
 * @author zzhao
 */
public class ExceptionHandler<S, X extends Throwable> extends DecoratingHandler<S, X> {

    private final BiConsumer<S, Throwable> exceptionConsumer;

    public ExceptionHandler(Handler<S, X> handler, BiConsumer<S, Throwable> exceptionConsumer) {
        super(handler);
        this.exceptionConsumer = exceptionConsumer;
    }

    public ExceptionHandler(Handler<S, X> handler) {
        this(handler, (s, x) -> Util.error("%s: %s", s, x));
    }

    @Override
    public void handle(S s) {
        try {
            super.handle(s);
        } catch (Throwable x) {
            this.exceptionConsumer.accept(s, x);
        }
    }
}
