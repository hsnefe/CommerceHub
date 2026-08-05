package com.commercehub.order.state;

import com.commercehub.order.entity.OrderStatus;

import java.util.EnumSet;
import java.util.Set;

public final class CreatedState implements OrderState {

    private static final Set<OrderStatus> ALLOWED = EnumSet.of(
            OrderStatus.STOCK_RESERVED,
            OrderStatus.CANCELLED
    );

    @Override
    public OrderStatus status() {
        return OrderStatus.CREATED;
    }

    @Override
    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.contains(target);
    }
}
