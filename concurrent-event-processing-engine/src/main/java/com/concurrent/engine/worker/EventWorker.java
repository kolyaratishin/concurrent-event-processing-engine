package com.concurrent.engine.worker;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.queue.EventQueue;

public class EventWorker implements Runnable{
    private final EventQueue eventQueue;
    private final EventHandler handler;
    private final String name;

    public EventWorker(EventQueue eventQueue, EventHandler handler, String name) {
        this.eventQueue = eventQueue;
        this.handler = handler;
        this.name = name;
    }

    private volatile boolean running = true;

    @Override
    public void run() {
        while (running)
        {
            Event event = eventQueue.poll();
            if (event != null)
            {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    System.out.println("Error happened while handling event in worker: " + name);
                }
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void stop() {
        running = false;
    }
}
