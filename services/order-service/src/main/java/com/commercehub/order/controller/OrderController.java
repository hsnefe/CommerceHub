package com.commercehub.order.controller;

import com.commercehub.order.dto.CancelOrderResponse;
import com.commercehub.order.dto.CreateOrderRequest;
import com.commercehub.order.dto.CreateOrderResponse;
import com.commercehub.order.dto.OrderDetailResponse;
import com.commercehub.order.dto.OrderSummaryResponse;
import com.commercehub.order.dto.UpdateOrderStatusRequest;
import com.commercehub.order.service.OrderService;
import com.commercehub.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<CreateOrderResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderResponse response = orderService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by id")
    public ResponseEntity<OrderDetailResponse> getById(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal JwtPrincipal principal,
            Authentication authentication
    ) {
        return ResponseEntity.ok(orderService.getById(orderId, principal.getId(), isAdmin(authentication)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List orders for a user")
    public ResponseEntity<List<OrderSummaryResponse>> listByUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal JwtPrincipal principal,
            Authentication authentication
    ) {
        return ResponseEntity.ok(orderService.listByUser(userId, principal.getId(), isAdmin(authentication)));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<CancelOrderResponse> cancel(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal JwtPrincipal principal,
            Authentication authentication
    ) {
        return ResponseEntity.ok(orderService.cancel(orderId, principal.getId(), isAdmin(authentication)));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Transition order status (ADMIN)")
    public ResponseEntity<OrderDetailResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                orderService.transitionStatus(orderId, request.status(), isAdmin(authentication))
        );
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
