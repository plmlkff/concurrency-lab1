package org.labs.logic.order;

import java.util.function.Consumer;

public record Order(
    Consumer<OrderState> clientCallback
) {
}
