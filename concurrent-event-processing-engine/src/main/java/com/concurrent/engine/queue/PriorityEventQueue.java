package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;
import java.util.HashMap;
import java.util.Map;

public class PriorityEventQueue implements EventQueue{
    private final int maxPriority = 10;
    private final Map<Integer, EventQueue> priorityMap = new HashMap<>();

    public PriorityEventQueue() {
        for (int i = 1; i <= maxPriority; i++) {
            priorityMap.put(i, new LockBasedQueue());
        }
    }

    @Override
    public void offer(Event event) {
        int priority = event.getType().getPriority();
        if (priority > 0 && priority <= maxPriority)
        {
            EventQueue lockBasedQueue = priorityMap.get(priority);
            lockBasedQueue.offer(event);
        }
    }

    @Override
    public Event poll() {
        for (int i = maxPriority; i >= 1; i--) {
            EventQueue lockBasedQueue = priorityMap.get(i);
            Event event = lockBasedQueue.poll();
            if (event != null)
            {
                return event;
            }
        }
        return null;
    }

    @Override
    public int size() {
        int size = 0;
        for (int i = 1; i <= maxPriority; i++) {
            int lockBaseQueueSize = priorityMap.get(i).size();
            size += lockBaseQueueSize;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 1; i <= maxPriority; i++) {
            if (!priorityMap.get(i).isEmpty()){
                return false;
            }
        }
        return true;
    }
}
