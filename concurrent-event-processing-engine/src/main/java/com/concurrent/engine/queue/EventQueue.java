package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;

public interface EventQueue {
    void offer(Event event);
    Event poll();
    int size();
    boolean isEmpty();
}
