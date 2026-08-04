package com.commercehub.order.state;

import com.commercehub.order.entity.OrderStatus;

public final class CancelledState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.CANCELLED;
    }

    @Override
    public boolean canTransitionTo(OrderStatus target) {
        return false;
    }
}
