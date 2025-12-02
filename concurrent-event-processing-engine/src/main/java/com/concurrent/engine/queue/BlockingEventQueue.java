package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingEventQueue implements EventQueue {
    private final LinkedList<Event> events = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final int capacity;

    public BlockingEventQueue(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void offer(Event event) {
        lock.lock();
        try {
            while (events.size() == capacity) {
                notFull.await();
            }
            events.addLast(event);
            notEmpty.signal();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Event poll() {
        Event result = null;
        lock.lock();
        try {
            while (events.isEmpty()) {
                notEmpty.await();
            }
            result = events.removeFirst();
            notFull.signal();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return result;
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
