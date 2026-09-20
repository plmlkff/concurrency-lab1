package org.labs.config;

import org.labs.domain.Programmer;
import org.labs.domain.Spoon;
import org.labs.domain.Waiter;
import org.labs.logic.Orchestrator;
import org.labs.logic.command.SignalChannel;
import org.labs.logic.metric.MetricsCollector;
import org.labs.logic.order.Order;
import org.labs.logic.order.OrderService;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class OrchestratorFactory {
    private OrchestratorFactory() {
    }

    public static Orchestrator create(
        OrchestratorConfig orchestratorConfig,
        ProgrammerConfig programmerConfig,
        WaiterConfig waiterConfig
    ) {
        var ordersQueue = new LinkedBlockingQueue<Order>();
        var orderService = new OrderService(ordersQueue);
        var dishesLeft = new AtomicInteger(orchestratorConfig.dishCapacity());

        var spoons = new ArrayList<Spoon>();
        for (int i = 0; i < orchestratorConfig.programmersCount(); i++) {
            spoons.add(new Spoon());
        }

        var channels = new ArrayList<SignalChannel>();
        var programmers = new ArrayList<Programmer>();
        for (int i = 0; i < orchestratorConfig.programmersCount(); i++) {
            var channel = new SignalChannel(new SynchronousQueue<>(), new SynchronousQueue<>());
            channels.add(channel);
            programmers.add(new Programmer(
                programmerConfig,
                spoons.get(i),
                spoons.get((i + 1) % spoons.size()),
                orderService,
                channel
            ));
        }

        var waiters = new ArrayList<Waiter>();
        for (int i = 0; i < orchestratorConfig.waitersCount(); i++) {
            waiters.add(new Waiter(waiterConfig, ordersQueue, dishesLeft));
        }

        var metricsCollector = new MetricsCollector(new PrintWriter(System.out));
        Runnable metricsTask = () -> metricsCollector.collect(
            programmers,
            ordersQueue,
            orchestratorConfig,
            programmerConfig,
            waiterConfig
        );

        return new Orchestrator(
            programmers,
            spoons,
            waiters,
            channels,
            metricsTask,
            orchestratorConfig.metricsPeriodMillis()
        );
    }
}
