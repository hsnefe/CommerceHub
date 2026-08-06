package com.commercehub.order.service;

import com.commercehub.messaging.DomainEventPublisher;
import com.commercehub.messaging.MessagingTopology;
import com.commercehub.messaging.event.OrderCancelledEvent;
import com.commercehub.messaging.event.OrderCreatedEvent;
import com.commercehub.messaging.event.OrderItemPayload;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final AuthClient authClient;
    private final DomainEventPublisher domainEventPublisher;
    private final OrderStateMachine orderStateMachine;

    public OrderService(
            OrderRepository orderRepository,
            ProductClient productClient,
            AuthClient authClient,
            DomainEventPublisher domainEventPublisher,
            OrderStateMachine orderStateMachine
    ) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.authClient = authClient;
        this.domainEventPublisher = domainEventPublisher;
        this.orderStateMachine = orderStateMachine;
    }

    @Transactional
    public CreateOrderResponse create(UUID userId, CreateOrderRequest request) {
        List<ResolvedItem> resolvedItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.items()) {
            ProductSnapshotResponse snapshot = productClient.getProductSnapshot(itemRequest.productId());
            BigDecimal unitPrice = snapshot.price().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            totalPrice = totalPrice.add(lineTotal);
            resolvedItems.add(new ResolvedItem(snapshot, itemRequest.quantity(), unitPrice, lineTotal));
        }

        String email = authClient.getUserEmail(userId);

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(totalPrice);

        for (ResolvedItem resolved : resolvedItems) {
            OrderItem item = new OrderItem();
            item.setProductId(resolved.snapshot().id());
            item.setProductName(resolved.snapshot().name());
            item.setUnitPrice(resolved.unitPrice());
            item.setQuantity(resolved.quantity());
            item.setLineTotal(resolved.lineTotal());
            order.addItem(item);
        }

        Order saved = orderRepository.save(order);

        List<OrderItemPayload> eventItems = resolvedItems.stream()
                .map(resolved -> new OrderItemPayload(resolved.snapshot().id(), resolved.quantity()))
                .toList();

        publishAfterCommit(
                MessagingTopology.ROUTING_ORDER_CREATED,
                new OrderCreatedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        saved.getId(),
                        userId,
                        email,
                        saved.getTotalPrice(),
                        eventItems
                )
        );

        return new CreateOrderResponse(
                saved.getId(),
                saved.getStatus().name(),
                saved.getTotalPrice(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getById(UUID orderId, UUID requesterId, boolean admin) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        assertCanAccess(order, requesterId, admin);
        return toDetail(order);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listByUser(UUID userId, UUID requesterId, boolean admin) {
        if (!admin && !userId.equals(requesterId)) {
            throw new ForbiddenException("Access denied");
        }
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> new OrderSummaryResponse(
                        order.getId(),
                        order.getStatus().name(),
                        order.getTotalPrice()
                ))
                .toList();
    }

    @Transactional
    public CancelOrderResponse cancel(UUID orderId, UUID requesterId, boolean admin) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        assertCanAccess(order, requesterId, admin);

        List<OrderItemPayload> eventItems = order.getItems().stream()
                .map(item -> new OrderItemPayload(item.getProductId(), item.getQuantity()))
                .toList();

        orderStateMachine.transition(order, OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        publishAfterCommit(
                MessagingTopology.ROUTING_ORDER_CANCELLED,
                new OrderCancelledEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        saved.getId(),
                        saved.getUserId(),
                        eventItems
                )
        );

        return new CancelOrderResponse(saved.getId(), saved.getStatus().name());
    }

    @Transactional
    public void markStockReserved(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.CREATED) {
            return;
        }
        orderStateMachine.transition(order, OrderStatus.STOCK_RESERVED);
        orderRepository.save(order);
    }

    @Transactional
    public void markPaid(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.PREPARING
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (order.getStatus() == OrderStatus.CREATED) {
            orderStateMachine.transition(order, OrderStatus.STOCK_RESERVED);
        }
        if (order.getStatus() == OrderStatus.STOCK_RESERVED) {
            orderStateMachine.transition(order, OrderStatus.PAID);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void cancelDueToPaymentFailure(UUID orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.PREPARING
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            return;
        }

        List<OrderItemPayload> eventItems = order.getItems().stream()
                .map(item -> new OrderItemPayload(item.getProductId(), item.getQuantity()))
                .toList();

        orderStateMachine.transition(order, OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        publishAfterCommit(
                MessagingTopology.ROUTING_ORDER_CANCELLED,
                new OrderCancelledEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        saved.getId(),
                        saved.getUserId(),
                        eventItems
                )
        );
    }

    @Transactional
    public OrderDetailResponse transitionStatus(UUID orderId, OrderStatus target, boolean admin) {
        if (!admin) {
            throw new ForbiddenException("Access denied");
        }
        if (target == OrderStatus.STOCK_RESERVED || target == OrderStatus.CANCELLED || target == OrderStatus.CREATED) {
            throw new ConflictException("Status " + target + " cannot be set via this endpoint");
        }

        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        orderStateMachine.transition(order, target);
        Order saved = orderRepository.save(order);
        return toDetail(saved);
    }

    private void publishAfterCommit(String routingKey, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    domainEventPublisher.publish(routingKey, event);
                }
            });
        } else {
            domainEventPublisher.publish(routingKey, event);
        }
    }

    private void assertCanAccess(Order order, UUID requesterId, boolean admin) {
        if (!admin && !order.getUserId().equals(requesterId)) {
            throw new ForbiddenException("Access denied");
        }
    }

    private OrderDetailResponse toDetail(Order order) {
        List<OrderDetailResponse.OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderDetailResponse.OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity()
                ))
                .toList();
        return new OrderDetailResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus().name(),
                order.getTotalPrice(),
                items
        );
    }

    private record ResolvedItem(
            ProductSnapshotResponse snapshot,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
