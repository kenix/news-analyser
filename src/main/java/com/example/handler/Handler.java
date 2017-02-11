/*
* Created at 19:21 on 10/02/2017
*/
package com.example.handler;

/**
 * @author zzhao
 */
public interface Handler<S, X extends Throwable> {
    void handle(S s) throws X;
}
