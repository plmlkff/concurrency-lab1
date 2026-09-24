package org.labs.domain;


import org.labs.config.ProgrammerConfig;
import org.labs.logic.Orchestrator.OrchestrationStrategy;
import org.labs.logic.command.Signal;
import org.labs.logic.command.SignalChannel;
import org.labs.logic.order.OrderService;
import org.labs.logic.order.OrderState;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Programmer implements Runnable {
    private static final int MIN = 0;
    private static final int MAX = 1;

    private volatile State state = State.DISCUSSING;
    private volatile int ateDishes = 0;
    private final ProgrammerConfig config;
    private final Spoon[] spoons = new Spoon[2];
    private final OrderService orderService;
    private final SignalChannel channel;
    private final BlockingQueue<OrderState> waiterChannel = new LinkedBlockingQueue<>();
    private final OrchestrationStrategy strategy;

    public Programmer(
        ProgrammerConfig config,
        Spoon left, Spoon right,
        OrderService orderService,
        SignalChannel signalChannel, OrchestrationStrategy strategy
    ) {
        this.config = config;
        this.strategy = strategy;
        this.spoons[MIN] = left.compareTo(right) < 0 ? left : right;
        this.spoons[MAX] = spoons[MIN].equals(left) ? right : left;
        this.orderService = orderService;
        this.channel = signalChannel;
    }

    @Override
    public void run() {
        try {
            init();
            while (state != State.STOPPED){
                switch (state) {
                    case DISCUSSING -> discuss();
                    case READY -> waitEatSignal();
                    case EATING -> eat();
                }
            }
        } catch (InterruptedException e) {
            System.out.printf("Поток <%s> прерван с ошибкой: %s\n", Thread.currentThread().getName(), e);
        }
    }

    private void init() throws InterruptedException {
        createOrder();
    }

    private void discuss() throws InterruptedException {
        Thread.sleep(config.discussionTime());
        state = State.READY;
    }

    private void waitEatSignal() throws InterruptedException {
        if (strategy == OrchestrationStrategy.FULL_CONCURRENCY) {
            state = State.EATING;
            return;
        }
        var signal = channel.input().take();
        if (!Signal.EAT.equals(signal)) throw new IllegalStateException("Signal <%s> is not supported in the input programmer channel.".formatted(signal));
        state = State.EATING;
    }

    private void eat() throws InterruptedException {
        var orderState = waitOrder();

        if (orderState.equals(OrderState.NO_FOOD)) {
            state = State.STOPPED;
            channel.output().put(Signal.NO_FOOD);
            return;
        }

        try {
            spoons[MIN].take();
            spoons[MAX].take();
            Thread.sleep(config.eatingTime());
            ateDishes++;
        } finally {
            spoons[MIN].put();
            spoons[MAX].put();
        }
        createOrder();
        if (strategy == OrchestrationStrategy.ROUNDS) channel.output().put(Signal.DONE);
        state = State.DISCUSSING;
    }

    private OrderState waitOrder() throws InterruptedException {
        return waiterChannel.take();
    }

    private void createOrder() throws InterruptedException {
        orderService.createOrder(ateDishes, waiterChannel::add);
    }

    public int getAteDishes() {
        return ateDishes;
    }

    public State getState() {
        return state;
    }

    public enum State {
        DISCUSSING,
        READY,
        EATING,
        STOPPED
    }
}
