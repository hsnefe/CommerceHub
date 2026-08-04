package com.commercehub.order.state;

import com.commercehub.order.entity.Order;
import com.commercehub.order.entity.OrderStatus;
import com.commercehub.order.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class OrderStateMachine {

    private final Map<OrderStatus, OrderState> states = new EnumMap<>(OrderStatus.class);

    public OrderStateMachine() {
        states.put(OrderStatus.CREATED, new CreatedState());
        states.put(OrderStatus.STOCK_RESERVED, new StockReservedState());
        states.put(OrderStatus.PAID, new PaidState());
        states.put(OrderStatus.PREPARING, new PreparingState());
        states.put(OrderStatus.SHIPPED, new ShippedState());
        states.put(OrderStatus.DELIVERED, new DeliveredState());
        states.put(OrderStatus.CANCELLED, new CancelledState());
    }

    public void transition(Order order, OrderStatus target) {
        OrderState current = states.get(order.getStatus());
        if (current == null || !current.canTransitionTo(target)) {
            throw new ConflictException(
                    "Cannot transition order from " + order.getStatus() + " to " + target
            );
        }
        order.setStatus(target);
    }

    public boolean canTransition(OrderStatus from, OrderStatus to) {
        OrderState current = states.get(from);
        return current != null && current.canTransitionTo(to);
    }
}
