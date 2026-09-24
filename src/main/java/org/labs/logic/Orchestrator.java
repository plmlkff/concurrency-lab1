package org.labs.logic;

import org.labs.domain.Programmer;
import org.labs.domain.Spoon;
import org.labs.domain.Waiter;
import org.labs.logic.command.Signal;
import org.labs.logic.command.SignalChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Orchestrator {
    private final List<Programmer> programmers;
    private final List<Spoon> spoons;
    private final List<Waiter> waiters;
    private final List<SignalChannel> channels;
    private final ExecutorService programmersExecutor;
    private final ExecutorService waitersExecutor;
    private final ExecutorService channelsExecutor;
    private final ScheduledExecutorService metricsCollectorExecutor;
    private final Runnable metricsTask;
    private final long metricsPeriodMillis;
    private final AtomicBoolean started = new AtomicBoolean();
    private final OrchestrationStrategy strategy;

    public Orchestrator(
        List<Programmer> programmers,
        List<Spoon> spoons,
        List<Waiter> waiters,
        List<SignalChannel> channels,
        Runnable metricsTask,
        long metricsPeriodMillis, OrchestrationStrategy strategy
    ) {
        this.programmers = List.copyOf(Objects.requireNonNull(programmers, "programmers"));
        this.spoons = List.copyOf(Objects.requireNonNull(spoons, "spoons"));
        this.waiters = List.copyOf(Objects.requireNonNull(waiters, "waiters"));
        this.channels = List.copyOf(Objects.requireNonNull(channels, "channels"));
        this.metricsTask = Objects.requireNonNull(metricsTask, "metricsTask");
        this.strategy = Objects.requireNonNull(strategy);
        if (metricsPeriodMillis <= 0) {
            throw new IllegalArgumentException("Metrics period must be positive");
        }
        this.metricsPeriodMillis = metricsPeriodMillis;

        if (this.programmers.size() < 2) {
            throw new IllegalArgumentException("At least two programmers are required");
        }
        if (this.spoons.size() != this.programmers.size()) {
            throw new IllegalArgumentException("Each programmer must have one spoon in the ring");
        }
        if (this.channels.size() != this.programmers.size()) {
            throw new IllegalArgumentException("Each programmer must have one signal channel");
        }
        if (this.waiters.isEmpty()) {
            throw new IllegalArgumentException("At least one waiter is required");
        }

        this.programmersExecutor = Executors.newFixedThreadPool(this.programmers.size());
        this.waitersExecutor = Executors.newFixedThreadPool(this.waiters.size());
        this.channelsExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.metricsCollectorExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void run() throws InterruptedException {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Orchestrator can only be run once");
        }

        try {
            startWorkers();
            metricsCollectorExecutor.scheduleWithFixedDelay(
                metricsTask,
                0,
                metricsPeriodMillis,
                TimeUnit.MILLISECONDS
            );

            switch (strategy) {
                case ROUNDS -> runRoundStrategy();
                case FULL_CONCURRENCY -> runFullConcurrencyStrategy();
            }

            printFinalMetrics();
        } finally {
            shutdownExecutors();
        }
    }

    private void runRoundStrategy() throws InterruptedException {
        var stopped = new boolean[programmers.size()];
        int start = 0;
        int remaining = programmers.size();

        while (remaining > 0) {
            remaining -= runRound(stopped, start);
            start = (start + 1) % programmers.size();
        }
    }

    private void runFullConcurrencyStrategy() {
        channels.stream().map(channel ->
            channelsExecutor.submit(() -> waitNoFoodSignal(channel))
        ).forEach(channel -> {
            try {
                channel.get();
            } catch (Exception ignored) {}
        });
    }

    private void startWorkers() {
        for (var programmer : programmers) {
            programmersExecutor.execute(programmer);
        }
        for (var waiter : waiters) {
            waitersExecutor.execute(waiter);
        }
    }

    private int runRound(boolean[] stopped, int start) throws InterruptedException {
        var pending = new boolean[programmers.size()];
        for (int i = 0; i < programmers.size(); i++) {
            pending[i] = !stopped[i];
        }

        int stoppedThisRound = 0;
        while (hasPending(pending)) {
            var batch = buildBatch(pending, start);
            for (int index : batch) {
                pending[index] = false;
            }
            stoppedThisRound += executeBatch(batch, stopped);
        }
        return stoppedThisRound;
    }

    private List<Integer> buildBatch(boolean[] pending, int start) {
        var selected = new boolean[programmers.size()];
        var batch = new ArrayList<Integer>();
        for (int step = 0; step < programmers.size(); step++) {
            int index = (start + step) % programmers.size();
            int left = (index + programmers.size() - 1) % programmers.size();
            int right = (index + 1) % programmers.size();
            if (pending[index] && !selected[left] && !selected[right]) {
                selected[index] = true;
                batch.add(index);
            }
        }
        return batch;
    }

    private int executeBatch(List<Integer> batch, boolean[] stopped) throws InterruptedException {
        var futures = new ArrayList<Future<Signal>>(batch.size());
        for (int index : batch) {
            futures.add(channelsExecutor.submit(() -> exchangeSignal(index)));
        }

        int newlyStopped = 0;
        for (int i = 0; i < batch.size(); i++) {
            int index = batch.get(i);
            var signal = waitForSignal(index, futures.get(i));
            if (signal == Signal.NO_FOOD) {
                stopped[index] = true;
                newlyStopped++;
            } else if (signal != Signal.DONE) {
                throw new IllegalStateException(
                    "Unexpected signal " + signal + " from programmer " + index
                );
            }
        }
        return newlyStopped;
    }

    private Signal exchangeSignal(int index) throws InterruptedException {
        channels.get(index).input().put(Signal.EAT);
        return channels.get(index).output().take();
    }

    private Signal waitNoFoodSignal(SignalChannel channel) {
        try {
            while (channel.output().take() != Signal.NO_FOOD);
            return Signal.NO_FOOD;
        } catch (InterruptedException ignored) {
            return Signal.NO_FOOD;
        }
    }

    private Signal waitForSignal(int index, Future<Signal> future) throws InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException failure) {
            throw new IllegalStateException(
                "Signal exchange failed for programmer " + index,
                failure.getCause()
            );
        }
    }

    private static boolean hasPending(boolean[] pending) {
        for (boolean value : pending) {
            if (value) {
                return true;
            }
        }
        return false;
    }

    private void printFinalMetrics() throws InterruptedException {
        metricsCollectorExecutor.shutdown();
        metricsCollectorExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        metricsTask.run();
    }

    private void shutdownExecutors() {
        metricsCollectorExecutor.shutdownNow();
        channelsExecutor.shutdownNow();
        programmersExecutor.shutdownNow();
        waitersExecutor.shutdownNow();
    }

    public enum OrchestrationStrategy {
        ROUNDS,
        FULL_CONCURRENCY
    }
}
