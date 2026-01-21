package com.concurrent.engine.metrics;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.events.EventType;
import com.concurrent.engine.handler.DeadLetterHandler;
import com.concurrent.engine.handler.RetryingEventHandler;
import com.concurrent.engine.queue.EventQueue;
import com.concurrent.engine.queue.SynchronizedQueue;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventEngineMetricsTest {

    /**
     * Фейковий хендлер, який падає перші N разів.
     */
    static class FailingNTimesHandler implements EventHandler {
        private final int failTimes;
        private final AtomicInteger calls = new AtomicInteger();

        FailingNTimesHandler(int failTimes) {
            this.failTimes = failTimes;
        }

        @Override
        public void handle(Event event) throws Exception {
            int c = calls.incrementAndGet();
            if (c <= failTimes) {
                throw new RuntimeException("fail attempt " + c);
            }
        }

        int getCalls() {
            return calls.get();
        }
    }

    @Test
    void testMetricsSuccessFailureDeadLetterRetry() throws Exception {
        EventEngineMetrics metrics = new EventEngineMetrics();

        EventQueue dlqQueue = new SynchronizedQueue();
        DeadLetterHandler dlqHandler = new DeadLetterHandler(dlqQueue, metrics);
        DeadLetterHandler dlqHandlerSpy = new DeadLetterHandler(dlqQueue, metrics) {
            @Override
            public void handle(Event event) {
                super.handle(event);
                metrics.recordDeadLetter();
            }
        };

        // Делегат падає 2 рази
        FailingNTimesHandler delegate = new FailingNTimesHandler(2);

        // Retrying handler обробляє подію
        EventHandler retrying = new RetryingEventHandler(
                event -> {
                    long start = System.nanoTime();
                    try {
                        delegate.handle(event);
                        metrics.recordSuccess(System.nanoTime() - start);
                    } catch (Exception e) {
                        metrics.recordFailure(System.nanoTime() - start);
                        throw e;
                    }
                },
                dlqHandlerSpy,
                3,
                1,
                metrics
        ) {
            @Override
            public void handle(Event event) throws Exception {
                try {
                    super.handle(event);
                } catch (Exception e) {
                    // RetryingEventHandler сам відправить у dead-letter
                }
            }
        };

        Event event = Event.of(EventType.ALERT, "payload");

        retrying.handle(event);

        // Делегат падає 2 рази + 1 успішний = 3 виклики
        assertEquals(3, delegate.getCalls(), "Delegate call count mismatch");

        // Повинно бути:
        // 2 failure, 1 success, 2 retries
        EventEngineMetricsSnapshot snap = metrics.snapshot(dlqQueue);

        System.out.println(snap);

        assertEquals(1, snap.processedOk(), "success count mismatch");
        assertEquals(2, snap.processedFailed(), "failure count mismatch");
        assertEquals(2, snap.retryCount(), "retry count mismatch");
        assertEquals(0, snap.deadLetter(), "dead-letter should be empty"); // успіх вмістився у 3 спроби

        // Час повинен бути > 0
        assertTrue(snap.maxProcessingMillis() >= 0);
        assertTrue(snap.avgProcessingMillis() >= 0);
    }

    @Test
    void testMaxAndAvgTime() {
        EventEngineMetrics metrics = new EventEngineMetrics();

        // Симулюємо обробку подій
        metrics.recordSuccess(1_000_000); // 1 ms
        metrics.recordSuccess(3_000_000); // 3 ms
        metrics.recordSuccess(2_000_000); // 2 ms

        EventQueue queue = new SynchronizedQueue();
        EventEngineMetricsSnapshot snap = metrics.snapshot(queue);

        assertEquals(3, snap.processedOk());
        assertEquals(3, snap.maxProcessingMillis());
        assertEquals(2, snap.avgProcessingMillis());
    }
}
