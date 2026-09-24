package org.labs.logic.order;

import java.util.function.Consumer;

public record Order(
    Consumer<OrderState> clientCallback,
    int priority
) implements Comparable<Order> {
    @Override
    public int compareTo(Order o) {
        return priority - o.priority;
    }
}
