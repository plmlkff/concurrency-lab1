package org.labs.e2e;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.labs.config.OrchestratorConfig;
import org.labs.config.OrchestratorFactory;
import org.labs.config.ProgrammerConfig;
import org.labs.config.WaiterConfig;
import org.labs.domain.Programmer;
import org.labs.logic.Orchestrator.OrchestrationStrategy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DiningSimulationE2ETest {
    private static final Pattern HEADER = Pattern.compile(
        "programmers=\\d+ \\| waiters="
    );
    private static final Pattern ROW = Pattern.compile(
        "(?m)^(\\d+)\\h+(DISCUSSING|READY|EATING|STOPPED)\\h+(\\d+)\\h*$"
    );
    private static final Pattern TOTAL = Pattern.compile("eaten=(\\d+) / total=(\\d+)");


    @ParameterizedTest(name = "{0} [{1}]")
    @CsvSource({
        "Two programmers share both spoons, ROUNDS, 2, 1, 20, 0, 0, 0",
        "Two programmers share both spoons, FULL_CONCURRENCY, 2, 1, 20, 0, 0, 0",
        "Small odd ring and indivisible stock, ROUNDS, 3, 2, 31, 0, 0, 0",
        "Small odd ring and indivisible stock, FULL_CONCURRENCY, 3, 2, 31, 0, 0, 0",
        "Seven programmers and two waiters, ROUNDS, 7, 2, 103, 1, 1, 1",
        "Seven programmers and two waiters, FULL_CONCURRENCY, 7, 2, 103, 1, 1, 1",
        "Even ring, ROUNDS, 8, 3, 128, 0, 0, 0",
        "Even ring, FULL_CONCURRENCY, 8, 3, 128, 0, 0, 0",
        "Empty restaurant stock, ROUNDS, 7, 2, 0, 0, 0, 0",
        "Empty restaurant stock, FULL_CONCURRENCY, 7, 2, 0, 0, 0, 0",
        "Fewer dishes than programmers, ROUNDS, 7, 2, 3, 0, 0, 0",
        "Fewer dishes than programmers, FULL_CONCURRENCY, 7, 2, 3, 0, 0, 0",
        "One slow waiter, ROUNDS, 7, 1, 71, 0, 0, 2",
        "One slow waiter, FULL_CONCURRENCY, 7, 1, 71, 0, 0, 2",
        "More waiters than programmers, ROUNDS, 7, 12, 71, 1, 1, 0",
        "More waiters than programmers, FULL_CONCURRENCY, 7, 12, 71, 1, 1, 0",
        "Ten thousand dishes with no delays do not lose food or starve participants, ROUNDS, 7, 4, 10000, 0, 0, 0",
        "Ten thousand dishes with no delays do not lose food or starve participants, FULL_CONCURRENCY, 7, 4, 10000, 0, 0, 0",
        "A full test according to the requirements with 7 programmers 2 waiters and a million dishes but without delays, ROUNDS, 7, 2, 1000000, 0, 0, 0",
        "A full test according to the requirements with 7 programmers 2 waiters and a million dishes but without delays, FULL_CONCURRENCY, 7, 2, 1000000, 0, 0, 0"
    })
    void finishesAndDistributesExactlyTheAvailableFood(
        String scenario, OrchestrationStrategy strategy, int programmers, int waiters, int dishes,
        int discuss, int eat, int prepare
    ) throws Exception {
        var reports = runSimulation(programmers, waiters, dishes, discuss, eat, prepare, 60_000, strategy);
        assertFinalReport(reports.getLast(), programmers, dishes, strategy);
    }

    private List<String> runSimulation(
        int programmers, int waiters, int dishes, int discuss,
        int eat, int prepare, long period, OrchestrationStrategy strategy
    ) throws Exception {
        var buffer = new ByteArrayOutputStream();
        var orchestrator = OrchestratorFactory.create(
            new OrchestratorConfig(programmers, waiters, dishes, period, strategy),
            new ProgrammerConfig(discuss, eat),
            new WaiterConfig(prepare),
            buffer
        );
        orchestrator.run();

        String output = buffer.toString(StandardCharsets.UTF_8);
        var matcher = HEADER.matcher(output);
        assertTrue(matcher.find(), "No metrics were printed:\n" + output);
        return List.of(output.split("(?=" + HEADER.pattern() + ")"));
    }

    private void assertFinalReport(String report, int programmers, int dishes, OrchestrationStrategy strategy) {
        var rows = readRows(report, programmers);
        assertTrue(rows.stream().allMatch(row -> row.state().equals(Programmer.State.STOPPED.name())),
            "The final report must describe finished programmers:\n" + report);
        assertEquals(dishes, rows.stream().mapToLong(Row::eaten).sum(), report);
        long min = rows.stream().mapToLong(Row::eaten).min().orElseThrow();
        long max = rows.stream().mapToLong(Row::eaten).max().orElseThrow();
        if (strategy == OrchestrationStrategy.ROUNDS) assertTrue(max - min <= 1, "Unfair food distribution:\n" + report);
        var totals = TOTAL.matcher(report);
        assertTrue(totals.find(), report);
        assertEquals(dishes, Long.parseLong(totals.group(1)));
        assertEquals(dishes, Long.parseLong(totals.group(2)));
        assertTrue(report.contains("orders: size=0"), "Orders remain after the meal:\n" + report);
    }

    private List<Row> readRows(String report, int expectedCount) {
        var rows = new ArrayList<Row>();
        var matcher = ROW.matcher(report);
        while (matcher.find()) {
            assertEquals(rows.size() + 1, Integer.parseInt(matcher.group(1)));
            rows.add(new Row(matcher.group(2), Long.parseLong(matcher.group(3))));
        }
        assertEquals(expectedCount, rows.size(), report);
        return rows;
    }

    private record Row(String state, long eaten) {}
}
