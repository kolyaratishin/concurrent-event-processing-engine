package com.concurrent.engine.metrics;

public record EventEngineMetricsSnapshot(
        long processedOk,
        long processedFailed,
        long deadLetter,
        long avgProcessingMillis,
        long maxProcessingMillis,
        long retryCount,
        int queueSize
) {
    @Override
    public String toString() {
        return """
                ====== Event Engine Metrics ======
                processed OK     : %d
                processed failed : %d
                dead-letter      : %d
                retries          : %d
                avg time (ms)    : %d
                max time (ms)    : %d
                queue size       : %d
                ==================================
                """.formatted(
                processedOk,
                processedFailed,
                deadLetter,
                retryCount,
                avgProcessingMillis,
                maxProcessingMillis,
                queueSize
        );
    }
}
