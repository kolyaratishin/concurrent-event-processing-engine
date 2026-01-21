package com.concurrent.engine.handler;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.metrics.EventEngineMetrics;
import com.concurrent.engine.queue.EventQueue;

public class DeadLetterHandler implements EventHandler {
    private final EventQueue deadLetterQueue;
    private final EventEngineMetrics metrics;

    public DeadLetterHandler(EventQueue deadLetterQueue, EventEngineMetrics metrics) {
        this.deadLetterQueue = deadLetterQueue;
        this.metrics = metrics;
    }

    @Override
    public void handle(Event event) {
        System.out.println("Event:" + event.getId() + " moved to dead-letter: " + event.getPayload());
        deadLetterQueue.offer(event);
        metrics.recordDeadLetter();
    }
}
