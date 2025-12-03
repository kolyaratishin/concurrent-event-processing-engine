package com.concurrent.engine.handler;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.queue.EventQueue;

public class DeadLetterHandler implements EventHandler {
    private final EventQueue deadLetterQueue;

    public DeadLetterHandler(EventQueue deadLetterQueue) {
        this.deadLetterQueue = deadLetterQueue;
    }

    @Override
    public void handle(Event event) {
        System.out.println("Event:" + event.getId() + " moved to dead-letter: " + event.getPayload());
        deadLetterQueue.offer(event);
    }
}
