package com.concurrent.engine.worker;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.metrics.EventEngineMetrics;
import com.concurrent.engine.queue.EventQueue;

public class EventWorker implements Runnable{
    private final EventQueue eventQueue;
    private final EventHandler handler;
    private final String name;
    private final EventEngineMetrics metrics;

    public EventWorker(EventQueue eventQueue, EventHandler handler, String name, EventEngineMetrics metrics) {
        this.eventQueue = eventQueue;
        this.handler = handler;
        this.name = name;
        this.metrics = metrics;
    }

    private volatile boolean running = true;

    @Override
    public void run() {
        while (running)
        {
            Event event = eventQueue.poll();
            if (event != null)
            {
                long start = System.nanoTime();
                try {
                    handler.handle(event);
                    long duration = System.nanoTime() - start;
                    metrics.recordSuccess(duration);
                } catch (Exception e) {
                    long duration = System.nanoTime() - start;
                    metrics.recordFailure(duration);
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
