package com.concurrent.engine.metrics;

import com.concurrent.engine.queue.EventQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EventEngineMetrics {
    private final AtomicLong processedOk = new AtomicLong();
    private final AtomicLong processedFailed = new AtomicLong();
    private final AtomicLong deadLetter = new AtomicLong();

    private final AtomicLong totalProcessingTimeNanos = new AtomicLong();
    private final AtomicLong maxProcessingTimeNanos = new AtomicLong();

    private final AtomicLong retryCount = new AtomicLong();

    public void recordSuccess(long nanos) {
        processedOk.incrementAndGet();
        totalProcessingTimeNanos.getAndAdd(nanos);
        updateMax(nanos);
    }

    public void recordFailure(long nanos) {
        processedFailed.incrementAndGet();
        totalProcessingTimeNanos.getAndAdd(nanos);
        updateMax(nanos);
    }

    public void recordDeadLetter() {
        deadLetter.incrementAndGet();
    }

    public void recordRetry() {
        retryCount.incrementAndGet();
    }

    private void updateMax(long newValue) {
        long prev;
        do {
            prev = maxProcessingTimeNanos.get();
            if (newValue <= prev) {
                return; // уже є більший max → нічого не робимо
            }
            // пробуємо оновити (CAS)
        } while (!maxProcessingTimeNanos.compareAndSet(prev, newValue));
    }

    public EventEngineMetricsSnapshot snapshot(EventQueue queue)
    {
        long ok = processedOk.get();
        long fail = processedFailed.get();
        long dlq = deadLetter.get();
        long retries = retryCount.get();

        long total = ok + fail;
        long avgMillis = total == 0
                ? 0
                : TimeUnit.NANOSECONDS.toMillis(totalProcessingTimeNanos.get() / total);

        long maxMillis = TimeUnit.NANOSECONDS.toMillis(maxProcessingTimeNanos.get());

        int queueSize = queue.size();

        return new EventEngineMetricsSnapshot(
                ok,
                fail,
                dlq,
                avgMillis,
                maxMillis,
                retries,
                queueSize
        );
    }

}
