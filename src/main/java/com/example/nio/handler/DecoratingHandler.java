/*
* Created at 04:22 on 11/02/2017
*/
package com.example.nio.handler;

/**
 * @author zzhao
 */
public abstract class DecoratingHandler<S, X extends Throwable> implements Handler<S, X> {

    private final Handler<S, X> handler;

    public DecoratingHandler(Handler<S, X> handler) {
        this.handler = handler;
    }

    @Override
    public void handle(S s) throws X {
        this.handler.handle(s);
    }
}
