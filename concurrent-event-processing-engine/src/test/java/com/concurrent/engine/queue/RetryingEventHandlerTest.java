package com.concurrent.engine.queue;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.events.EventType;
import com.concurrent.engine.handler.DeadLetterHandler;
import com.concurrent.engine.handler.RetryingEventHandler;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetryingEventHandlerTest {

    /**
     * Хендлер, який падає перші N разів, а потім успішно обробляє подію.
     */
    static class FailingNTimesHandler implements EventHandler {
        private final int failTimes;
        private final AtomicInteger calls = new AtomicInteger();

        FailingNTimesHandler(int failTimes) {
            this.failTimes = failTimes;
        }

        @Override
        public void handle(Event event) throws Exception {
            int call = calls.incrementAndGet();
            if (call <= failTimes) {
                throw new RuntimeException("Fail #" + call);
            }
            // інакше — успіх, нічого не робимо
        }

        int getCalls() {
            return calls.get();
        }
    }

    /**
     * DeadLetterHandler, який просто рахує, скільки подій туди потрапило.
     */
    static class CountingDeadLetterHandler extends DeadLetterHandler {

        private final AtomicInteger dlqCount = new AtomicInteger();

        public CountingDeadLetterHandler(EventQueue deadLetterQueue) {
            super(deadLetterQueue);
        }

        @Override
        public void handle(Event event) {
            super.handle(event);
            dlqCount.incrementAndGet();
        }

        int getDlqCount() {
            return dlqCount.get();
        }
    }

    @Test
    void testSuccessAfterRetries_NoDeadLetter() throws Exception {
        // handler падає перші 2 рази, потім успіх
        FailingNTimesHandler delegate = new FailingNTimesHandler(2);

        EventQueue dlqQueue = new SynchronizedQueue();
        CountingDeadLetterHandler dlqHandler = new CountingDeadLetterHandler(dlqQueue);

        int maxAttempts = 5;
        long delayMillis = 1L;

        RetryingEventHandler retrying =
                new RetryingEventHandler(delegate, dlqHandler, maxAttempts, delayMillis);

        Event event = Event.of(EventType.LOG, "payload");

        retrying.handle(event);

        // делегат має бути викликаний 3 рази: 2 фейли + 1 успіх
        assertEquals(3, delegate.getCalls(), "Delegate call count mismatch");

        // у dead-letter нічого не повинно піти
        assertEquals(0, dlqHandler.getDlqCount(), "Dead-letter should be empty");
        assertEquals(0, dlqQueue.size(), "Dead-letter queue size should be 0");
    }

    @Test
    void testAlwaysFail_GoesToDeadLetter() throws Exception {
        // handler завжди падає
        FailingNTimesHandler delegate = new FailingNTimesHandler(Integer.MAX_VALUE);

        EventQueue dlqQueue = new SynchronizedQueue();
        CountingDeadLetterHandler dlqHandler = new CountingDeadLetterHandler(dlqQueue);

        int maxAttempts = 3;
        long delayMillis = 1L;

        RetryingEventHandler retrying =
                new RetryingEventHandler(delegate, dlqHandler, maxAttempts, delayMillis);

        Event event = Event.of(EventType.ALERT, "critical");

        retrying.handle(event);

        // делегат має бути викликаний рівно maxAttempts разів
        assertEquals(maxAttempts, delegate.getCalls(), "Delegate call count mismatch");

        // у dead-letter повинна бути рівно 1 подія
        assertEquals(1, dlqHandler.getDlqCount(), "Dead-letter should contain 1 event");
        assertEquals(1, dlqQueue.size(), "Dead-letter queue size should be 1");
    }
}
