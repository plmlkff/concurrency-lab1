package org.labs;

import org.labs.config.OrchestratorConfig;
import org.labs.config.OrchestratorFactory;
import org.labs.config.ProgrammerConfig;
import org.labs.config.WaiterConfig;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        var orchestratorConfig = new OrchestratorConfig(7, 2, 1_000_000, 1_000);
        var programmerConfig = new ProgrammerConfig(10, 20);
        var waiterConfig = new WaiterConfig(10);

        var orchestrator = OrchestratorFactory.create(orchestratorConfig, programmerConfig, waiterConfig);
        orchestrator.run();
    }
}
