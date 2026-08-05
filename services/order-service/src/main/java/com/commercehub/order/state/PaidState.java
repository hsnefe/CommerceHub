package com.commercehub.order.state;

import com.commercehub.order.entity.OrderStatus;

import java.util.EnumSet;
import java.util.Set;

public final class PaidState implements OrderState {

    private static final Set<OrderStatus> ALLOWED = EnumSet.of(OrderStatus.PREPARING);

    @Override
    public OrderStatus status() {
        return OrderStatus.PAID;
    }

    @Override
    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.contains(target);
    }
}
