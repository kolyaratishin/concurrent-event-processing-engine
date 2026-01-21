package com.concurrent.engine.handler;

import com.concurrent.engine.events.Event;
import com.concurrent.engine.events.EventHandler;
import com.concurrent.engine.metrics.EventEngineMetrics;

public class RetryingEventHandler implements EventHandler {
    private final EventHandler delegate;         // справжній хендлер
    private final DeadLetterHandler deadLetterHandler;
    private final int maxAttempts;              // скільки разів пробувати
    private final long delayMillis;             // пауза між спробами
    private final EventEngineMetrics metrics;

    public RetryingEventHandler(EventHandler delegate, DeadLetterHandler deadLetterHandler, int maxAttempts, long delayMillis, EventEngineMetrics metrics) {
        this.delegate = delegate;
        this.deadLetterHandler = deadLetterHandler;
        this.maxAttempts = maxAttempts;
        this.delayMillis = delayMillis;
        this.metrics = metrics;
    }


    @Override
    public void handle(Event event) throws Exception {
        int attempt = 0;
        while (true) {
            try {
                delegate.handle(event);
                return; // успішно – виходимо
            } catch (Exception e) {
                attempt++;
                metrics.recordRetry();
                if (maxAttemptsExceeded(event, attempt)) return;
                if (successfulAfterSleep(event)) return;
            }
        }
    }

    private boolean maxAttemptsExceeded(Event event, int attempt) {
        if (attempt >= maxAttempts) {
            // всі спроби вичерпані – відправляємо в dead-letter
            try {
                deadLetterHandler.handle(event);
            } catch (Exception de) {
                System.out.println("Error on retry " + attempt + " for event: " + event.getId());
            }
            return true;
        }
        return false;
    }

    private boolean successfulAfterSleep(Event event) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            // при interrupt — теж відправляємо в dead-letter і виходимо
            deadLetterHandler.handle(event);
            return true;
        }
        return false;
    }
}
