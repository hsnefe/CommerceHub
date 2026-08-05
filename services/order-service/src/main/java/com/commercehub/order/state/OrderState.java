package com.commercehub.order.state;

import com.commercehub.order.entity.OrderStatus;

public interface OrderState {

    OrderStatus status();

    boolean canTransitionTo(OrderStatus target);
}
