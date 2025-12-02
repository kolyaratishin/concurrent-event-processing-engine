package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LockBasedQueue implements EventQueue {
    private final LinkedList<Event> events = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    @Override
    public void offer(Event event) {
        lock.lock();
        try {
            events.addLast(event);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Event poll() {
        lock.lock();
        try {
            return isEmpty() ? null : events.removeFirst();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return events.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return events.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}
