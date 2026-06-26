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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private InventoryService inventoryService;

    private final UUID productId = UUID.fromString("a1000000-0000-4000-8000-000000000001");

    private InventoryItem sampleItem() {
        InventoryItem item = new InventoryItem();
        item.setProductId(productId);
        item.setAvailableQuantity(10);
        item.setLowStockThreshold(5);
        return item;
    }

    @Nested
    class Create {

        @Test
        void create_success() {
            when(inventoryItemRepository.existsById(productId)).thenReturn(false);
            doNothing().when(productClient).validateProductExists(productId);
            when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> {
                InventoryItem item = invocation.getArgument(0);
                return item;
            });

            InventoryResponse response = inventoryService.create(new InventoryRequest(productId, 10, 5));

            assertThat(response.productId()).isEqualTo(productId);
            assertThat(response.availableQuantity()).isEqualTo(10);
            assertThat(response.lowStock()).isFalse();
        }

        @Test
        void create_duplicateProduct_throwsConflict() {
            when(inventoryItemRepository.existsById(productId)).thenReturn(true);

            assertThatThrownBy(() -> inventoryService.create(new InventoryRequest(productId, 10, 5)))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    class GetByProductId {

        @Test
        void getByProductId_notFound() {
            when(inventoryItemRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.getByProductId(productId))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void getByProductId_lowStockFlag() {
            InventoryItem item = sampleItem();
            item.setAvailableQuantity(3);
            when(inventoryItemRepository.findById(productId)).thenReturn(Optional.of(item));

            InventoryResponse response = inventoryService.getByProductId(productId);

            assertThat(response.lowStock()).isTrue();
        }
    }

    @Nested
    class List {

        @Test
        void list_returnsPage() {
            when(inventoryItemRepository.findAll(eq(PageRequest.of(0, 20))))
                    .thenReturn(new PageImpl<>(List.of(sampleItem()), PageRequest.of(0, 20), 1));

            InventoryPageResponse page = inventoryService.list(0, 20);

            assertThat(page.content()).hasSize(1);
            assertThat(page.totalElements()).isEqualTo(1);
        }
    }

    @Nested
    class Decrement {

        @Test
        void decrement_insufficientStock_throwsConflict() {
            InventoryItem item = sampleItem();
            item.setAvailableQuantity(2);
            when(inventoryItemRepository.findById(productId)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> inventoryService.decrement(productId, 5))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void decrement_success() {
            InventoryItem item = sampleItem();
            when(inventoryItemRepository.findById(productId)).thenReturn(Optional.of(item));
            when(inventoryItemRepository.save(item)).thenReturn(item);

            InventoryResponse response = inventoryService.decrement(productId, 3);

            assertThat(response.availableQuantity()).isEqualTo(7);
            verify(inventoryItemRepository).save(item);
        }
    }

    @Nested
    class Increment {

        @Test
        void increment_success() {
            InventoryItem item = sampleItem();
            when(inventoryItemRepository.findById(productId)).thenReturn(Optional.of(item));
            when(inventoryItemRepository.save(item)).thenReturn(item);

            InventoryResponse response = inventoryService.increment(productId, 5);

            assertThat(response.availableQuantity()).isEqualTo(15);
        }
    }

    @Nested
    class Update {

        @Test
        void update_success() {
            InventoryItem item = sampleItem();
            when(inventoryItemRepository.findById(productId)).thenReturn(Optional.of(item));
            when(inventoryItemRepository.save(item)).thenReturn(item);

            InventoryResponse response = inventoryService.update(
                    productId,
                    new InventoryUpdateRequest(20, 8)
            );

            assertThat(response.availableQuantity()).isEqualTo(20);
            assertThat(response.lowStockThreshold()).isEqualTo(8);
        }
    }
}
