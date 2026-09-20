package org.labs.domain;

import org.labs.config.WaiterConfig;
import org.labs.logic.order.Order;
import org.labs.logic.order.OrderState;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Waiter implements Runnable{
    private final WaiterConfig cfg;
    private final BlockingQueue<Order> ordersQueue;
    private final AtomicInteger dishCapacity;

    public Waiter(WaiterConfig cfg, BlockingQueue<Order> ordersQueue, AtomicInteger dishCapacity) {
        this.cfg = cfg;
        this.ordersQueue = ordersQueue;
        this.dishCapacity = dishCapacity;
    }

    @Override
    public void run() {
        try {
            while (true) {
                var order = ordersQueue.take();

                if (dishCapacity.getAndDecrement() <= 0) {
                    order.clientCallback().accept(OrderState.NO_FOOD);
                    continue;
                }

                Thread.sleep(cfg.orderPreparingTime());

                order.clientCallback().accept(OrderState.READY);
            }
        } catch (InterruptedException e) {
            System.err.printf("Ошибка получения заказа в потоке: %s\n", Thread.currentThread().getName());
        }

    }
}
