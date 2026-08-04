package com.commercehub.inventory.service;

import com.commercehub.inventory.client.ProductClient;
import com.commercehub.inventory.dto.InventoryPageResponse;
import com.commercehub.inventory.dto.InventoryRequest;
import com.commercehub.inventory.dto.InventoryResponse;
import com.commercehub.inventory.dto.InventoryUpdateRequest;
import com.commercehub.inventory.entity.InventoryItem;
import com.commercehub.inventory.exception.ConflictException;
import com.commercehub.inventory.exception.NotFoundException;
import com.commercehub.inventory.repository.InventoryItemRepository;
import com.commercehub.messaging.event.OrderItemPayload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ProductClient productClient;

    public InventoryService(InventoryItemRepository inventoryItemRepository, ProductClient productClient) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.productClient = productClient;
    }

    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        if (inventoryItemRepository.existsById(request.productId())) {
            throw new ConflictException("Inventory record already exists for product");
        }

        productClient.validateProductExists(request.productId());

        InventoryItem item = new InventoryItem();
        item.setProductId(request.productId());
        item.setAvailableQuantity(request.availableQuantity());
        item.setLowStockThreshold(request.lowStockThreshold());

        return toResponse(inventoryItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        InventoryItem item = inventoryItemRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Inventory record not found"));
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryItem> result = inventoryItemRepository.findAll(pageable);

        return new InventoryPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Transactional
    public InventoryResponse update(UUID productId, InventoryUpdateRequest request) {
        InventoryItem item = inventoryItemRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Inventory record not found"));

        if (request.availableQuantity() != null) {
            item.setAvailableQuantity(request.availableQuantity());
        }
        if (request.lowStockThreshold() != null) {
            item.setLowStockThreshold(request.lowStockThreshold());
        }

        return toResponse(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryResponse decrement(UUID productId, int amount) {
        InventoryItem item = inventoryItemRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Inventory record not found"));

        if (item.getAvailableQuantity() < amount) {
            throw new ConflictException("Insufficient stock");
        }

        item.setAvailableQuantity(item.getAvailableQuantity() - amount);
        return toResponse(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryResponse increment(UUID productId, int amount) {
        InventoryItem item = inventoryItemRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Inventory record not found"));

        item.setAvailableQuantity(item.getAvailableQuantity() + amount);
        return toResponse(inventoryItemRepository.save(item));
    }

    @Transactional
    public void reserve(List<OrderItemPayload> items) {
        for (OrderItemPayload line : items) {
            InventoryItem item = inventoryItemRepository.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Inventory record not found for product " + line.productId()));

            if (item.getAvailableQuantity() < line.quantity()) {
                throw new ConflictException("Insufficient stock for product " + line.productId());
            }

            item.setAvailableQuantity(item.getAvailableQuantity() - line.quantity());
            item.setReservedQuantity(item.getReservedQuantity() + line.quantity());
            inventoryItemRepository.save(item);
        }
    }

    @Transactional
    public void release(List<OrderItemPayload> items) {
        for (OrderItemPayload line : items) {
            InventoryItem item = inventoryItemRepository.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Inventory record not found for product " + line.productId()));

            if (item.getReservedQuantity() < line.quantity()) {
                throw new ConflictException("Insufficient reserved stock for product " + line.productId());
            }

            item.setReservedQuantity(item.getReservedQuantity() - line.quantity());
            item.setAvailableQuantity(item.getAvailableQuantity() + line.quantity());
            inventoryItemRepository.save(item);
        }
    }

    private InventoryResponse toResponse(InventoryItem item) {
        boolean lowStock = item.getAvailableQuantity() <= item.getLowStockThreshold();
        return new InventoryResponse(
                item.getProductId(),
                item.getAvailableQuantity(),
                item.getReservedQuantity(),
                item.getLowStockThreshold(),
                lowStock,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
