package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;
import java.util.LinkedList;

public class SynchronizedQueue implements EventQueue {
    private final LinkedList<Event> events = new LinkedList<>();
    private final Object lock = new Object();

    @Override
    public void offer(Event event) {
        synchronized (lock) {
            events.addLast(event);
        }
    }

    @Override
    public Event poll() {
        synchronized (lock) {
            return isEmpty() ? null : events.removeFirst();
        }
    }

    @Override
    public int size() {
        synchronized (lock) {
            return events.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (lock) {
            {
                return events.isEmpty();
            }
        }
    }
}