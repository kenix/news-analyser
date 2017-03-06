/*
* Created at 20:15 on 27/02/2017
*/
package com.example.nio.event;

import com.example.Util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Receives selectable channels, monitors them using a selector and dispatches NIO events to the event handler.
 * <ul>
 * <li>should work like AWT event queue</li>
 * </ul>
 *
 * @author zzhao
 */
public class NioEventHub implements Closeable {

    private static final CompletableFuture<Boolean> NOT_INVOKED = CompletableFuture.completedFuture(Boolean.FALSE);

    private volatile boolean stopped = false;

    private final Thread selectorThread = new Thread(this::run, "nio-event-hub");

    private final Selector selector;

    private final AtomicBoolean selectorWaked = new AtomicBoolean(false);

    private final BlockingQueue<SelectorTaskWrapper> selectorTaskQueue;

    public NioEventHub() throws IOException {
        this.selector = Selector.open();
        this.selectorTaskQueue = new LinkedBlockingQueue<>(Util.getIntConfig("actionQueueSize", 64));
    }

    public void start() {
        this.selectorThread.start();
    }

    private void run() {
        try {
            while (!this.stopped) {
                invokeSelectorTasks();
                if (select() > 0) {
                    final Set<SelectionKey> selectedKeys = this.selector.selectedKeys();
                    for (Iterator<SelectionKey> it = selectedKeys.iterator(); it.hasNext(); ) {
                        final SelectionKey selectionKey = it.next();
                        it.remove();
                        handleSelectedKeys(selectionKey);
                    }
                }
                this.selectorWaked.set(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Util.close(this.selector);
        }
    }

    private void handleSelectedKeys(SelectionKey selectionKey) {
        if (!selectionKey.isValid()) {
            return;
        }

        final int iopsBefore = selectionKey.interestOps();
        if (selectionKey.isWritable()) {
            //todo
        }
        throw new UnsupportedOperationException();
    }

    private int select() throws IOException {
        // todo w/o timeout, use selection strategies
        try {
            return this.selector.select();
        } finally {
            invokeSelectorTasks();
        }
    }

    private void invokeSelectorTasks() {
        SelectorTaskWrapper task;
        while ((task = this.selectorTaskQueue.poll()) != null) {
            try {
                task.selectorTask.exec();
            } catch (Throwable e) {
                task.future.completeExceptionally(e);
            }
        }
    }

    private CompletableFuture<Boolean> invokeLater(SelectorTask r) {
        final SelectorTaskWrapper task;
        if (r == null || this.stopped || !this.selectorTaskQueue.offer(task = new SelectorTaskWrapper(r))) {
            return NOT_INVOKED;
        }

        try {
            return task.future;
        } finally {
            if (this.selectorWaked.compareAndSet(false, true)) {
                this.selector.wakeup();
            }
        }
    }

    public CompletableFuture<Boolean> register(SelectableChannel sc, int interestOps) {
        return invokeLater(() -> sc.register(this.selector, interestOps));
    }

    public CompletableFuture<Boolean> unregister(SelectableChannel sc, boolean closeChannel) {
        return invokeLater(() -> {
            final SelectionKey selKey = sc.keyFor(this.selector);
            if (selKey != null) {
                selKey.cancel();
                if (closeChannel) {
                    Util.close(sc);
                }
            }
        });
    }

    private interface SelectorTask {
        void exec() throws Throwable;
    }

    private static class SelectorTaskWrapper {

        private final CompletableFuture<Boolean> future;

        private final SelectorTask selectorTask;

        public SelectorTaskWrapper(SelectorTask selectorTask) {
            this.future = new CompletableFuture<>();
            this.selectorTask = selectorTask;
        }
    }

    @Override
    public void close() throws IOException {
        this.stopped = true;
        try {
            this.selectorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}
