package com.commercehub.order.state;

import com.commercehub.order.entity.Order;
import com.commercehub.order.entity.OrderStatus;
import com.commercehub.order.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    @Test
    void allowsHappyPathTransitions() {
        Order order = orderWith(OrderStatus.CREATED);
        stateMachine.transition(order, OrderStatus.STOCK_RESERVED);
        stateMachine.transition(order, OrderStatus.PAID);
        stateMachine.transition(order, OrderStatus.PREPARING);
        stateMachine.transition(order, OrderStatus.SHIPPED);
        stateMachine.transition(order, OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void allowsCancelFromCreatedAndStockReserved() {
        Order created = orderWith(OrderStatus.CREATED);
        stateMachine.transition(created, OrderStatus.CANCELLED);
        assertThat(created.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order reserved = orderWith(OrderStatus.STOCK_RESERVED);
        stateMachine.transition(reserved, OrderStatus.CANCELLED);
        assertThat(reserved.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void rejectsInvalidTransitions() {
        Order paid = orderWith(OrderStatus.PAID);
        assertThatThrownBy(() -> stateMachine.transition(paid, OrderStatus.CANCELLED))
                .isInstanceOf(ConflictException.class);

        Order created = orderWith(OrderStatus.CREATED);
        assertThatThrownBy(() -> stateMachine.transition(created, OrderStatus.PAID))
                .isInstanceOf(ConflictException.class);

        Order delivered = orderWith(OrderStatus.DELIVERED);
        assertThatThrownBy(() -> stateMachine.transition(delivered, OrderStatus.SHIPPED))
                .isInstanceOf(ConflictException.class);
    }

    private Order orderWith(OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(status);
        return order;
    }
}
