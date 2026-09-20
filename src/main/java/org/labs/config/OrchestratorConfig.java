package org.labs.config;

public record OrchestratorConfig(
    int programmersCount,
    int waitersCount,
    int dishCapacity
) {
}
