package org.labs.logic;

import org.labs.domain.Programmer;
import org.labs.domain.Spoon;
import org.labs.domain.Waiter;
import org.labs.logic.command.SignalChannel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Orchestrator {
    private final List<Programmer> programmers;
    private final List<Spoon> spoons;
    private final List<Waiter> waiters;
    private final List<SignalChannel> channels;
    private final ExecutorService programmersExecutor;
    private final ExecutorService waitersExecutor;

    public Orchestrator(
        List<Programmer> programmers,
        List<Spoon> spoons,
        List<Waiter> waiters,
        List<SignalChannel> channels
    ) {
        this.programmers = programmers;
        this.spoons = spoons;
        this.waiters = waiters;
        this.channels = channels;
        this.programmersExecutor = Executors.newFixedThreadPool(programmers.size());
        this.waitersExecutor = Executors.newFixedThreadPool(waiters.size());
    }
}
