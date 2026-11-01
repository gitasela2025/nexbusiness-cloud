package com.nexlora.nexbusiness.agent;

import com.nexlora.nexbusiness.entity.PosOrder;
import com.nexlora.nexbusiness.entity.ProductInventory;
import com.nexlora.nexbusiness.repository.PosOrderRepository;
import com.nexlora.nexbusiness.repository.ProductInventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaceOrderToolTest {

    @Mock private ProductInventoryRepository inventoryRepository;
    @Mock private PosOrderRepository orderRepository;

    @InjectMocks private PlaceOrderTool tool;

    private ProductInventory cups(int stock) {
        ProductInventory p = new ProductInventory();
        p.setId(2L);
        p.setName("Paper Cups (50pk)");
        p.setSku("SKU-002");
        p.setPrice(new BigDecimal("4.20"));
        p.setStockQty(stock);
        p.setBranchId("COLOMBO");
        return p;
    }

    @Test
    void notEnoughStock_orderIsNotSaved() {
        ProductInventory p = cups(3);
        when(inventoryRepository.findByNameContainingIgnoreCase("Paper Cups")).thenReturn(List.of(p));

        String result = tool.execute(Map.of("productName", "Paper Cups", "quantity", 50));

        assertTrue(result.contains("Not enough stock"));
        assertEquals(3, p.getStockQty());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void enoughStock_savesOrder_andReducesStock() {
        ProductInventory p = cups(3);
        when(inventoryRepository.findByNameContainingIgnoreCase("Paper Cups")).thenReturn(List.of(p));
        when(orderRepository.save(any(PosOrder.class))).thenAnswer(i -> i.getArgument(0));

        String result = tool.execute(Map.of("productName", "Paper Cups", "quantity", 2));

        assertTrue(result.contains("\"placed\":true"));
        assertEquals(1, p.getStockQty());
        verify(orderRepository, times(1)).save(any(PosOrder.class));
    }

    @Test
    void invalidQuantity_isRejected() {
        String result = tool.execute(Map.of("productName", "Paper Cups", "quantity", 0));

        assertTrue(result.contains("quantity must be"));
        verifyNoInteractions(orderRepository);
    }
}
