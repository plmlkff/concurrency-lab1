package org.labs;

import org.labs.config.OrchestratorConfig;
import org.labs.config.OrchestratorFactory;
import org.labs.config.ProgrammerConfig;
import org.labs.config.WaiterConfig;
import org.labs.logic.Orchestrator.OrchestrationStrategy;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        var orchestratorConfig = new OrchestratorConfig(7, 2, 1_000_000, 1_000, OrchestrationStrategy.FULL_CONCURRENCY);
        var programmerConfig = new ProgrammerConfig(0, 0);
        var waiterConfig = new WaiterConfig(0);

        var orchestrator = OrchestratorFactory.create(orchestratorConfig, programmerConfig, waiterConfig);
        var startTime = System.currentTimeMillis();
        orchestrator.run();
        System.out.printf("Execution time: %sms", System.currentTimeMillis() - startTime);
    }
}
