package org.labs.logic.metric;

import org.labs.config.OrchestratorConfig;
import org.labs.config.ProgrammerConfig;
import org.labs.config.WaiterConfig;
import org.labs.domain.Programmer;
import org.labs.logic.order.Order;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class MetricsCollector {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final PrintWriter out;

    public MetricsCollector(PrintWriter out) {
        this.out = out;
    }

    public void collect(
        List<Programmer> programmers,
        BlockingQueue<Order> ordersQueue,
        OrchestratorConfig orchestratorConfig,
        ProgrammerConfig programmerConfig,
        WaiterConfig waiterConfig
    ) {
        var states = new Programmer.State[programmers.size()];
        var eatenDishes = new int[programmers.size()];
        long totalEaten = 0;
        for (int i = 0; i < programmers.size(); i++) {
            var programmer = programmers.get(i);
            states[i] = programmer.getState();
            eatenDishes[i] = programmer.getAteDishes();
            totalEaten += eatenDishes[i];
        }

        int queueSize = ordersQueue.size();

        var report = new StringBuilder();
        report.append(String.format(
            "programmers=%d | waiters=%d | discuss=%dms | eat=%dms | prepare=%dms%n",
            orchestratorConfig.programmersCount(),
            orchestratorConfig.waitersCount(),
            programmerConfig.discussionTime(),
            programmerConfig.eatingTime(),
            waiterConfig.orderPreparingTime()
        ));
        report.append("#  state        ateDishes\n");
        for (int i = 0; i < programmers.size(); i++) {
            report.append(String.format(
                "%-5d%-13s%d%n",
                i + 1,
                states[i],
                eatenDishes[i]
            ));
        }
        report.append(String.format(
            "eaten=%d / total=%d%n",
            totalEaten,
            orchestratorConfig.dishCapacity()
        ));
        appendQueueMetrics(report, queueSize);

        out.print(report);
        out.flush();
    }

    private void appendQueueMetrics(
        StringBuilder report,
        int size
    ) {
        report.append(String.format(
            "%s | orders: size=%d",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            size
        ));
    }
}
