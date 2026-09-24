package org.labs.logic.order;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class OrderService {
    private final BlockingQueue<Order> ordersQueue;

    public OrderService(BlockingQueue<Order> ordersQueue) {
        this.ordersQueue = ordersQueue;
    }

    public void createOrder(int priority, Consumer<OrderState> callback) throws InterruptedException {
        ordersQueue.put(new Order(callback, priority));
    }
}
