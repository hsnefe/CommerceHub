package com.commercehub.order.state;

import com.commercehub.order.entity.OrderStatus;

import java.util.EnumSet;
import java.util.Set;

public final class StockReservedState implements OrderState {

    private static final Set<OrderStatus> ALLOWED = EnumSet.of(
            OrderStatus.PAID,
            OrderStatus.CANCELLED
    );

    @Override
    public OrderStatus status() {
        return OrderStatus.STOCK_RESERVED;
    }

    @Override
    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.contains(target);
    }
}
