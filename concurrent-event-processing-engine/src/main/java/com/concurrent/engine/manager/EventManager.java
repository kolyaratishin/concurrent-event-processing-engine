package com.concurrent.engine.manager;

import com.concurrent.engine.events.DispatchingEventHandler;
import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.events.EventType;
import com.concurrent.engine.metrics.EventEngineMetrics;
import com.concurrent.engine.metrics.EventEngineMetricsSnapshot;
import com.concurrent.engine.queue.EventQueue;
import com.concurrent.engine.worker.EventWorkerPool;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventManager {
    private final EventQueue eventQueue;
    private final DispatchingEventHandler dispatchingHandler;
    private final EventWorkerPool workerPool;
    private final AtomicBoolean running;
    private final EventEngineMetrics metrics;

    public EventManager(EventQueue eventQueue, int workerCount, String poolName, EventEngineMetrics metrics)
    {
        this.eventQueue = eventQueue;
        this.dispatchingHandler = new DispatchingEventHandler();
        this.workerPool = new EventWorkerPool(workerCount, eventQueue, dispatchingHandler, poolName, metrics);
        this.running = new AtomicBoolean(false);
        this.metrics = metrics;
    }

    public synchronized void start()
    {
        if (running.get()){
            return;
        }
        workerPool.start();
        running.set(true);
    }

    public synchronized void stop()
    {
        if (!running.get()){
            return;
        }
        workerPool.stop();
        running.set(false);
    }

    public void registerHandler(EventType type, EventHandler handler)
    {
        dispatchingHandler.registerHandler(type, handler);
    }

    public void publish(Event event){
        eventQueue.offer(event);
    }

    public void publish(EventType type, String payload){
        Event event = Event.of(type, payload);
        publish(event);
    }

    public EventEngineMetricsSnapshot getMetrics() {
        return metrics.snapshot(eventQueue);
    }

}
