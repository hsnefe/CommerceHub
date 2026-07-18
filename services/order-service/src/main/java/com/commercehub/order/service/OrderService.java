package com.commercehub.order.service;

import com.commercehub.order.client.AuthClient;
import com.commercehub.order.client.InventoryClient;
import com.commercehub.order.client.NotificationClient;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final AuthClient authClient;
    private final NotificationClient notificationClient;

    public OrderService(
            OrderRepository orderRepository,
            ProductClient productClient,
            InventoryClient inventoryClient,
            AuthClient authClient,
            NotificationClient notificationClient
    ) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
        this.authClient = authClient;
        this.notificationClient = notificationClient;
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

        List<ResolvedItem> decremented = new ArrayList<>();
        try {
            for (ResolvedItem resolved : resolvedItems) {
                inventoryClient.decrement(resolved.snapshot().id(), resolved.quantity());
                decremented.add(resolved);
            }
        } catch (RuntimeException ex) {
            compensate(decremented);
            throw ex;
        }

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
        try {
            String email = authClient.getUserEmail(userId);
            notificationClient.sendOrderCreated(email, saved.getId());
        } catch (RuntimeException ex) {
            // Notification is best-effort; the order is already persisted.
        }
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

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Order is already cancelled");
        }
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ConflictException("Order cannot be cancelled");
        }

        for (OrderItem item : order.getItems()) {
            inventoryClient.increment(item.getProductId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return new CancelOrderResponse(saved.getId(), saved.getStatus().name());
    }

    private void compensate(List<ResolvedItem> decremented) {
        for (int i = decremented.size() - 1; i >= 0; i--) {
            ResolvedItem item = decremented.get(i);
            try {
                inventoryClient.increment(item.snapshot().id(), item.quantity());
            } catch (RuntimeException ignored) {
                // Best-effort compensation; original failure is rethrown by caller.
            }
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
