package org.labs.domain;


import org.labs.config.ProgrammerConfig;
import org.labs.logic.command.Signal;
import org.labs.logic.command.SignalChannel;
import org.labs.logic.order.OrderService;
import org.labs.logic.order.OrderState;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

public class Programmer implements Runnable {
    private static final int LEFT = 0;
    private static final int RIGHT = 1;

    private volatile State state = State.DISCUSSING;
    private volatile int ateDishes = 0;
    private final ProgrammerConfig config;
    private final Spoon[] spoons = new Spoon[2];
    private final OrderService orderService;
    private final SignalChannel channel;
    private final BlockingQueue<OrderState> waiterChannel = new LinkedBlockingQueue<>();

    public Programmer(
        ProgrammerConfig config,
        Spoon left, Spoon right,
        OrderService orderService,
        SignalChannel signalChannel
    ) {
        this.config = config;
        this.spoons[LEFT] = left;
        this.spoons[RIGHT] = right;
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
        var signal = channel.input().take();
        if (!Signal.EAT.equals(signal)) throw new IllegalStateException("Signal <%s> is not supported in the input programmer channel.".formatted(signal));
        state = State.EATING;
    }

    private void eat() throws InterruptedException {
        var orderState = waitOrder();

        if (orderState.equals(OrderState.NO_FOOD)) {
            channel.output().put(Signal.NO_FOOD);
            state = State.STOPPED;
            return;
        }

        try {
            spoons[LEFT].take();
            spoons[RIGHT].take();
            Thread.sleep(config.eatingTime());
            ateDishes++;
        } finally {
            spoons[LEFT].put();
            spoons[RIGHT].put();
        }
        createOrder();
        channel.output().put(Signal.DONE);
        state = State.DISCUSSING;
    }

    private OrderState waitOrder() throws InterruptedException {
        return waiterChannel.take();
    }

    private void createOrder() throws InterruptedException {
        orderService.createOrder(waiterChannel::add);
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
