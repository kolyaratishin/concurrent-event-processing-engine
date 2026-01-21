package com.concurrent.engine.worker;

import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.metrics.EventEngineMetrics;
import com.concurrent.engine.queue.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventWorkerPool {
    private final List<EventWorker> workers;
    private final ExecutorService executor;
    private final int workerCount;
    private final EventQueue eventQueue;
    private final EventHandler handler;
    private final String poolName;
    private final AtomicBoolean running;
    private final EventEngineMetrics metrics;

    public EventWorkerPool(int workerCount,
                           EventQueue eventQueue,
                           EventHandler handler,
                           String poolName,
                           EventEngineMetrics metrics)
    {
        this.workerCount = workerCount;
        this.eventQueue = eventQueue;
        this.handler = handler;
        this.poolName = poolName;
        this.running = new AtomicBoolean();
        this.running.set(false);
        this.executor = Executors.newFixedThreadPool(workerCount);
        this.workers = new ArrayList<>();
        this.metrics = metrics;
    }

    public synchronized void start(){
        if (running.get())
        {
            return;
        }
        for (int i = 0; i < workerCount; i++) {
            EventWorker eventWorker = new EventWorker(eventQueue, handler, poolName + "-worker-" + i, metrics);
            workers.add(eventWorker);
            executor.submit(eventWorker);
        }
        running.set(true);
    }

    public synchronized void stop(){
        if (!running.get())
        {
            return;
        }
        running.set(false);
        for (int i = 0; i < workerCount; i++) {
            workers.get(i).stop();
        }
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

}
