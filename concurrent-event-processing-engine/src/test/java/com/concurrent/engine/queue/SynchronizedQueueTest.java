package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventType;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SynchronizedQueueTest {

    @Test
    public void testConcurrentOfferAndPoll() throws InterruptedException {
        EventQueue queue = new PriorityEventQueue();

        int producerThreads = 10;
        int consumerThreads = 10;
        int eventsPerProducer = 1000;

        int totalExpectedEvents = producerThreads * eventsPerProducer;

        ExecutorService executor = Executors.newFixedThreadPool(producerThreads + consumerThreads);

        ConcurrentLinkedQueue<String> producedIds = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> consumedIds = new ConcurrentLinkedQueue<>();

        CountDownLatch producersDone = new CountDownLatch(producerThreads);
        CountDownLatch consumersDone = new CountDownLatch(consumerThreads);

        // PRODUCERS
        for (int i = 0; i < producerThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerProducer; j++) {
                        Event event = Event.of(EventType.LOG, "payload");
                        producedIds.add(event.getId());
                        queue.offer(event);
                    }
                } finally {
                    producersDone.countDown();
                }
            });
        }

        // CONSUMERS
        for (int i = 0; i < consumerThreads; i++) {
            executor.submit(() -> {
                try {
                    while (true) {
                        // stop when all events consumed
                        if (consumedIds.size() >= totalExpectedEvents) {
                            break;
                        }

                        Event event = queue.poll();
                        if (event != null) {
                            consumedIds.add(event.getId());
                        }
                    }
                } finally {
                    consumersDone.countDown();
                }
            });
        }

        // WAIT FOR PRODUCERS FINISH
        producersDone.await();

        // WAIT FOR CONSUMERS TO FINISH
        consumersDone.await();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // -------------------------------
        // VALIDATION
        // -------------------------------

        assertEquals(totalExpectedEvents, producedIds.size(),
                "Number of produced events mismatch");

        assertEquals(totalExpectedEvents, consumedIds.size(),
                "Number of consumed events mismatch");

        // Check uniqueness
        Set<String> uniqueProduced = new HashSet<>(producedIds);
        Set<String> uniqueConsumed = new HashSet<>(consumedIds);

        assertEquals(totalExpectedEvents, uniqueProduced.size(),
                "Duplicate IDs found in produced events");

        assertEquals(totalExpectedEvents, uniqueConsumed.size(),
                "Duplicate IDs found in consumed events");

        // Check that all produced events were consumed
        assertEquals(uniqueProduced, uniqueConsumed,
                "Mismatch between produced and consumed events");

        // Queue should be empty
        assertTrue(queue.isEmpty(), "Queue should be empty after consumption");
        assertEquals(0, queue.size(), "Queue size should be zero");

        System.out.println("Test PASSED: All events produced/consumed correctly & thread-safe.");
    }
}
