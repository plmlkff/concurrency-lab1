package org.labs.config;

import org.labs.logic.Orchestrator;

public record OrchestratorConfig(
    int programmersCount,
    int waitersCount,
    int dishCapacity,
    long metricsPeriodMillis,
    Orchestrator.OrchestrationStrategy strategy
) {
    public OrchestratorConfig {
        if (metricsPeriodMillis <= 0) {
            throw new IllegalArgumentException("Metrics period must be positive");
        }
    }
}
