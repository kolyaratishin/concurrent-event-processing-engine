package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class LockFreeEventQueue implements EventQueue {
    private final AtomicReferenceArray<Event> buffer;
    private final int capacity;          // реальний розмір буфера
    private final AtomicLong head = new AtomicLong(0); // індекс читання
    private final AtomicLong tail = new AtomicLong(0); // індекс запису

    public LockFreeEventQueue(int capacity) {
        // capacity краще зробити степенем двійки (наприклад 1024), але це не обов’язково
        this.capacity = capacity;
        this.buffer = new AtomicReferenceArray<>(capacity);
    }

    @Override
    public void offer(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        while (true) {
            long t = tail.get();
            long h = head.get();

            // bounded check: скільки елементів "зарезервовано/в черзі"
            if (t - h >= capacity) {
                return; // full
            }

            // Спроба зарезервувати слот: тільки один producer виграє CAS на поточному t
            if (tail.compareAndSet(t, t + 1)) {
                int idx = (int) (t % capacity);

                // Публікуємо event у зарезервований слот
                buffer.set(idx, event);

                return;
            }
        }
    }

    @Override
    public Event poll() {
        long t = tail.get();
        long h = head.get();
        // bounded check: скільки елементів "зарезервовано/в черзі"
        if (t == h) {
            return null; // empty
        }
        int index = (int) h % capacity;
        Event event = buffer.get(index);
        if (event == null) {
            return null;
        }
        buffer.set(index, null);
        head.lazySet(h + 1);
        return event;
    }

    @Override
    public int size() {
        long h = head.get();
        long t = tail.get();
        return (int) (t - h);
    }

    @Override
    public boolean isEmpty() {
        return head.get() == tail.get();
    }
}
