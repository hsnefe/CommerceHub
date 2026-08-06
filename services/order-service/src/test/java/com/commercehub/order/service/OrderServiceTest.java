package com.commercehub.order.service;

import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.OrderCancelledEvent;
import com.commercehub.messaging.event.OrderCreatedEvent;
import com.commercehub.order.client.AuthClient;
import com.commercehub.order.client.ProductClient;
import com.commercehub.order.dto.CancelOrderResponse;
import com.commercehub.order.dto.CreateOrderRequest;
import com.commercehub.order.dto.CreateOrderResponse;
import com.commercehub.order.dto.OrderDetailResponse;
import com.commercehub.order.dto.OrderSummaryResponse;
import com.commercehub.order.dto.ProductSnapshotResponse;
import com.commercehub.order.entity.Order;
import com.commercehub.order.entity.OrderItem;
import com.commercehub.order.entity.OrderStatus;
import com.commercehub.order.exception.ConflictException;
import com.commercehub.order.exception.ForbiddenException;
import com.commercehub.order.exception.NotFoundException;
import com.commercehub.order.repository.OrderRepository;
import com.commercehub.order.state.OrderStateMachine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private AuthClient authClient;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Spy
    private OrderStateMachine orderStateMachine = new OrderStateMachine();

    @InjectMocks
    private OrderService orderService;

    private final UUID userId = UUID.fromString("c1000000-0000-4000-8000-000000000001");
    private final UUID otherUserId = UUID.fromString("c1000000-0000-4000-8000-000000000099");
    private final UUID productId = UUID.fromString("a1000000-0000-4000-8000-000000000001");
    private final UUID orderId = UUID.fromString("d1000000-0000-4000-8000-000000000001");

    private ProductSnapshotResponse mouseSnapshot() {
        return new ProductSnapshotResponse(productId, "Gaming Mouse", new BigDecimal("799.99"));
    }

    private Order sampleOrder() {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(new BigDecimal("1599.98"));
        order.setCreatedAt(Instant.parse("2026-06-22T10:00:00Z"));
        order.setUpdatedAt(Instant.parse("2026-06-22T10:00:00Z"));

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setProductId(productId);
        item.setProductName("Gaming Mouse");
        item.setUnitPrice(new BigDecimal("799.99"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("1599.98"));
        order.addItem(item);
        return order;
    }

    @Nested
    class Create {

        @Test
        void create_success_publishesOrderCreatedEvent() {
            when(productClient.getProductSnapshot(productId)).thenReturn(mouseSnapshot());
            when(authClient.getUserEmail(userId)).thenReturn("user@example.com");
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                if (order.getId() == null) {
                    order.setId(orderId);
                }
                if (order.getCreatedAt() == null) {
                    order.setCreatedAt(Instant.parse("2026-06-22T10:00:00Z"));
                }
                return order;
            });

            CreateOrderResponse response = orderService.create(
                    userId,
                    new CreateOrderRequest(List.of(new CreateOrderRequest.OrderItemRequest(productId, 2)))
            );

            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.status()).isEqualTo("CREATED");
            assertThat(response.totalPrice()).isEqualByComparingTo("1599.98");

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getItems()).hasSize(1);

            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(domainEventPublisher).publish(eq(MessagingTopology.ROUTING_ORDER_CREATED), eventCaptor.capture());
            assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
            assertThat(eventCaptor.getValue().email()).isEqualTo("user@example.com");
            assertThat(eventCaptor.getValue().items()).hasSize(1);
        }

        @Test
        void create_productNotFound_throwsNotFound() {
            when(productClient.getProductSnapshot(productId)).thenThrow(new NotFoundException("Product not found"));

            assertThatThrownBy(() -> orderService.create(
                    userId,
                    new CreateOrderRequest(List.of(new CreateOrderRequest.OrderItemRequest(productId, 1)))
            )).isInstanceOf(NotFoundException.class);

            verify(orderRepository, never()).save(any());
            verify(domainEventPublisher, never()).publish(any(), any());
        }
    }

    @Nested
    class GetById {

        @Test
        void getById_owner_success() {
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(sampleOrder()));

            OrderDetailResponse response = orderService.getById(orderId, userId, false);

            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.items()).hasSize(1);
            assertThat(response.items().getFirst().productName()).isEqualTo("Gaming Mouse");
        }

        @Test
        void getById_otherUser_throwsForbidden() {
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(sampleOrder()));

            assertThatThrownBy(() -> orderService.getById(orderId, otherUserId, false))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void getById_admin_canAccessOtherUsersOrder() {
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(sampleOrder()));

            OrderDetailResponse response = orderService.getById(orderId, otherUserId, true);

            assertThat(response.orderId()).isEqualTo(orderId);
        }

        @Test
        void getById_notFound() {
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getById(orderId, userId, false))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class ListByUser {

        @Test
        void listByUser_owner_success() {
            when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(sampleOrder()));

            List<OrderSummaryResponse> responses = orderService.listByUser(userId, userId, false);

            assertThat(responses).hasSize(1);
            assertThat(responses.getFirst().status()).isEqualTo("CREATED");
        }

        @Test
        void listByUser_otherUser_throwsForbidden() {
            assertThatThrownBy(() -> orderService.listByUser(userId, otherUserId, false))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class Cancel {

        @Test
        void cancel_created_publishesOrderCancelledEvent() {
            Order order = sampleOrder();
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            CancelOrderResponse response = orderService.cancel(orderId, userId, false);

            assertThat(response.status()).isEqualTo("CANCELLED");
            verify(domainEventPublisher).publish(eq(MessagingTopology.ROUTING_ORDER_CANCELLED), any(OrderCancelledEvent.class));
            verify(orderRepository).save(order);
        }

        @Test
        void cancel_stockReserved_publishesOrderCancelledEvent() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.STOCK_RESERVED);
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            CancelOrderResponse response = orderService.cancel(orderId, userId, false);

            assertThat(response.status()).isEqualTo("CANCELLED");
            verify(domainEventPublisher).publish(eq(MessagingTopology.ROUTING_ORDER_CANCELLED), any(OrderCancelledEvent.class));
        }

        @Test
        void cancel_alreadyCancelled_throwsConflict() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancel(orderId, userId, false))
                    .isInstanceOf(ConflictException.class);

            verify(domainEventPublisher, never()).publish(any(), any());
        }

        @Test
        void cancel_paid_throwsConflict() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.PAID);
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancel(orderId, userId, false))
                    .isInstanceOf(ConflictException.class);

            verify(domainEventPublisher, never()).publish(any(), any());
        }

        @Test
        void cancel_otherUser_throwsForbidden() {
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(sampleOrder()));

            assertThatThrownBy(() -> orderService.cancel(orderId, otherUserId, false))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class TransitionStatus {

        @Test
        void transitionStatus_admin_paidFromStockReserved() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.STOCK_RESERVED);
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            OrderDetailResponse response = orderService.transitionStatus(orderId, OrderStatus.PAID, true);

            assertThat(response.status()).isEqualTo("PAID");
        }

        @Test
        void transitionStatus_nonAdmin_throwsForbidden() {
            assertThatThrownBy(() -> orderService.transitionStatus(orderId, OrderStatus.PAID, false))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    class MarkStockReserved {

        @Test
        void markStockReserved_fromCreated() {
            Order order = sampleOrder();
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            orderService.markStockReserved(orderId);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
            verify(orderRepository).save(order);
        }

        @Test
        void markStockReserved_ignoresNonCreated() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            orderService.markStockReserved(orderId);

            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    class SagaPayment {

        @Test
        void markPaid_fromStockReserved() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.STOCK_RESERVED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            orderService.markPaid(orderId);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(orderRepository).save(order);
        }

        @Test
        void markPaid_fromCreated_transitionsThroughStockReserved() {
            Order order = sampleOrder();
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            orderService.markPaid(orderId);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        void markPaid_alreadyPaid_isIdempotent() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.PAID);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            orderService.markPaid(orderId);

            verify(orderRepository, never()).save(any());
        }

        @Test
        void cancelDueToPaymentFailure_fromStockReserved_publishesCancelled() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.STOCK_RESERVED);
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            orderService.cancelDueToPaymentFailure(orderId);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(domainEventPublisher).publish(eq(MessagingTopology.ROUTING_ORDER_CANCELLED), any(OrderCancelledEvent.class));
        }

        @Test
        void cancelDueToPaymentFailure_alreadyCancelled_isIdempotent() {
            Order order = sampleOrder();
            order.setStatus(OrderStatus.CANCELLED);
            when(orderRepository.findWithItemsById(orderId)).thenReturn(Optional.of(order));

            orderService.cancelDueToPaymentFailure(orderId);

            verify(domainEventPublisher, never()).publish(any(), any());
            verify(orderRepository, never()).save(any());
        }
    }
}
