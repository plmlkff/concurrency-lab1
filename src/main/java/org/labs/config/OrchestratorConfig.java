package org.labs.config;

public record OrchestratorConfig(
    int programmersCount,
    int waitersCount,
    int dishCapacity,
    long metricsPeriodMillis
) {
    public OrchestratorConfig {
        if (metricsPeriodMillis <= 0) {
            throw new IllegalArgumentException("Metrics period must be positive");
        }
    }
}
