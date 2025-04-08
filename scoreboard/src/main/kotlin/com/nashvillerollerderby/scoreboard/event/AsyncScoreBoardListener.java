package com.nashvillerollerderby.scoreboard.event;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AsyncScoreBoardListener extends Thread implements ScoreBoardListener {
    public AsyncScoreBoardListener(ScoreBoardListener l) {
        this.listener = l;
        listeners.add(this);
        start();
    }

    @Override
    public void scoreBoardChange(ScoreBoardEvent<?> event) {
        synchronized (this) {
            queue.add(event);
            this.notifyAll();
        }
    }

    @Override
    public void run() {
        while (true) {
            ScoreBoardEvent<?> event = null;
            synchronized (this) {
                try {
                    if ((event = queue.poll()) == null) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                }
            }
            if (event != null) {
                listener.scoreBoardChange(event);
            }
        }
    }

    private final ScoreBoardListener listener;
    private final Queue<ScoreBoardEvent<?>> queue = new LinkedList<>();

    private static final Queue<AsyncScoreBoardListener> listeners = new ConcurrentLinkedQueue<>();
}
