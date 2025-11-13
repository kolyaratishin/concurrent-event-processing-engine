package com.concurrent.engine.events;

public enum EventType {
    LOG(2), METRIC(1), ALERT(3);
    private final int priority;

    EventType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
