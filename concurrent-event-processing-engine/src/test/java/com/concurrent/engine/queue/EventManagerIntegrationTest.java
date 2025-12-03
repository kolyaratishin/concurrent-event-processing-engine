package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.events.EventType;
import com.concurrent.engine.manager.EventManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EventManagerIntegrationTest {
    /**
     * Простий хендлер, який просто рахує, скільки разів його викликали.
     */
    static class CountingHandler implements EventHandler {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public void handle(Event event) {
            counter.incrementAndGet();
        }

        int getCount() {
            return counter.get();
        }
    }

    @Test
    void testEventManagerWithManyProducersAndWorkers() throws Exception {
        // ---- Конфіг ----
        int workerCount = 8;          // скільки воркерів-споживачів
        int producerThreads = 20;     // скільки потоків-продюсерів
        int iterationsPerProducer = 2000; // скільки разів кожен продюсер публікує набір подій

        int expectedPerType = producerThreads * iterationsPerProducer;

        // ---- Черга + менеджер ----
        EventQueue queue = new PriorityEventQueue();
        EventManager manager = new EventManager(queue, workerCount, "integration-pool");

        // ---- Хендлери, які рахують виклики ----
        CountingHandler logHandler = new CountingHandler();
        CountingHandler metricHandler = new CountingHandler();
        CountingHandler alertHandler = new CountingHandler();

        manager.registerHandler(EventType.LOG, logHandler);
        manager.registerHandler(EventType.METRIC, metricHandler);
        manager.registerHandler(EventType.ALERT, alertHandler);

        manager.start();

        // ---- Продюсери ----
        ExecutorService producers = Executors.newFixedThreadPool(producerThreads);
        CountDownLatch producersDone = new CountDownLatch(producerThreads);

        for (int i = 0; i < producerThreads; i++) {
            producers.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerProducer; j++) {
                        // кожен продюсер публікує по одному івенту кожного типу
                        manager.publish(EventType.LOG, "log payload");
                        manager.publish(EventType.METRIC, "metric payload");
                        manager.publish(EventType.ALERT, "alert payload");
                    }
                } finally {
                    producersDone.countDown();
                }
            });
        }

        // чекаємо, поки всі продюсери закінчать публікувати
        producers.shutdown();
        boolean producersFinished = producers.awaitTermination(60, TimeUnit.SECONDS);
        assertTrue(producersFinished, "Producers did not finish in time");

        // додатково переконуємося, що latch впав до нуля
        producersDone.await(60, TimeUnit.SECONDS);

        // ---- Чекаємо, поки воркери все переварять ----
        long deadline = System.currentTimeMillis() + 60_000; // до 60 секунд
        while ((logHandler.getCount() < expectedPerType
                || metricHandler.getCount() < expectedPerType
                || alertHandler.getCount() < expectedPerType)
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        // зупиняємо систему
        manager.stop();

        // ---- Ассерти ----
        assertEquals(expectedPerType, logHandler.getCount(),
                "LOG events handled count mismatch");
        assertEquals(expectedPerType, metricHandler.getCount(),
                "METRIC events handled count mismatch");
        assertEquals(expectedPerType, alertHandler.getCount(),
                "ALERT events handled count mismatch");
    }
}
