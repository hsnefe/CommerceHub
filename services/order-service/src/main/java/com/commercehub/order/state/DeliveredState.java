package com.commercehub.order.state;

import com.commercehub.order.entity.OrderStatus;

public final class DeliveredState implements OrderState {

    @Override
    public OrderStatus status() {
        return OrderStatus.DELIVERED;
    }

    @Override
    public boolean canTransitionTo(OrderStatus target) {
        return false;
    }
}
