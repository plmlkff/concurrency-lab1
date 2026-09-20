package org.labs;

import org.labs.config.OrchestratorConfig;
import org.labs.config.OrchestratorFactory;
import org.labs.config.ProgrammerConfig;
import org.labs.config.WaiterConfig;

public class Main {
    public static void main(String[] args) {
        var orchestratorConfig = new OrchestratorConfig(7, 2, 1_000_000);
        var programmerConfig = new ProgrammerConfig(1_000, 1_000);
        var waiterConfig = new WaiterConfig(1_000);

        var orchestrator = OrchestratorFactory.create(orchestratorConfig, programmerConfig, waiterConfig);
    }
}
