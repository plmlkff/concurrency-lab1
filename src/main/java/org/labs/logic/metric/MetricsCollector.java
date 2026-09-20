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
import java.util.Locale;
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
        int remainingCapacity = ordersQueue.remainingCapacity();
        long queueCapacity = (long) queueSize + remainingCapacity;

        var report = new StringBuilder();
        report.append(String.format(
            Locale.ROOT,
            "programmers=%d | waiters=%d | discuss=%dms | eat=%dms | prepare=%dms%n",
            orchestratorConfig.programmersCount(),
            orchestratorConfig.waitersCount(),
            programmerConfig.discussionTime(),
            programmerConfig.eatingTime(),
            waiterConfig.orderPreparingTime()
        ));
        report.append(String.format(
            Locale.ROOT,
            "%s | eaten=%d / total=%d%n",
            LocalDateTime.now().format(TIMESTAMP_FORMAT),
            totalEaten,
            orchestratorConfig.dishCapacity()
        ));
        appendQueueMetrics(report, queueSize, remainingCapacity, queueCapacity);
        report.append("#  state        ateDishes\n");
        for (int i = 0; i < programmers.size(); i++) {
            report.append(String.format(
                Locale.ROOT,
                "%-3d%-13s%d%n",
                i + 1,
                states[i],
                eatenDishes[i]
            ));
        }

        out.print(report);
        out.flush();
    }

    private void appendQueueMetrics(
        StringBuilder report,
        int size,
        int remainingCapacity,
        long capacity
    ) {
        if (capacity >= Integer.MAX_VALUE) {
            report.append(String.format(
                Locale.ROOT,
                "orders: size=%d | remaining=%d | capacity=unbounded | occupancy=n/a%n",
                size,
                remainingCapacity
            ));
            return;
        }

        if (capacity == 0) {
            report.append(String.format(
                Locale.ROOT,
                "orders: size=%d | remaining=%d | capacity≈0 | occupancy=n/a%n",
                size,
                remainingCapacity
            ));
            return;
        }

        double occupancy = 100.0 * size / capacity;
        report.append(String.format(
            Locale.ROOT,
            "orders: size=%d | remaining=%d | capacity≈%d | occupancy≈%.1f%%%n",
            size,
            remainingCapacity,
            capacity,
            occupancy
        ));
    }
}
