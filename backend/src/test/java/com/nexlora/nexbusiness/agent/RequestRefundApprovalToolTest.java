package com.nexlora.nexbusiness.agent;

import com.nexlora.nexbusiness.entity.PendingApproval;
import com.nexlora.nexbusiness.entity.PosOrder;
import com.nexlora.nexbusiness.entity.ProductInventory;
import com.nexlora.nexbusiness.repository.PosOrderRepository;
import com.nexlora.nexbusiness.repository.ProductInventoryRepository;
import com.nexlora.nexbusiness.service.ApprovalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestRefundApprovalToolTest {

    @Mock private ApprovalService approvalService;
    @Mock private PosOrderRepository orderRepository;
    @Mock private ProductInventoryRepository inventoryRepository;

    @InjectMocks private RequestRefundApprovalTool tool;

    private PosOrder completedOrder() {
        PosOrder o = new PosOrder();
        o.setId(1L);
        o.setUserId(1L);
        o.setProductId(2L);
        o.setQuantity(2);
        o.setTotalPrice(new BigDecimal("8.40"));
        o.setStatus(PosOrder.OrderStatus.COMPLETED);
        return o;
    }

    @Test
    void validRequest_createsPendingApproval_notARefund() {
        ProductInventory p = new ProductInventory();
        p.setId(2L);
        p.setName("Paper Cups (50pk)");
        PendingApproval saved = new PendingApproval();
        saved.setId(5L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(completedOrder()));
        when(inventoryRepository.findById(2L)).thenReturn(Optional.of(p));
        when(approvalService.findPendingDuplicate(any(), anyString(), anyLong())).thenReturn(Optional.empty());
        when(approvalService.createApprovalRequest(eq(PendingApproval.ActionType.REFUND), anyString(), anyString()))
            .thenReturn(saved);

        String result = tool.execute(Map.of("orderId", 1, "reason", "damaged"));

        assertTrue(result.contains("PENDING"));
        verify(approvalService).createApprovalRequest(eq(PendingApproval.ActionType.REFUND), anyString(), anyString());
    }

    @Test
    void duplicateRequest_doesNotCreateAnother() {
        PendingApproval existing = new PendingApproval();
        existing.setId(9L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(completedOrder()));
        when(approvalService.findPendingDuplicate(any(), anyString(), anyLong())).thenReturn(Optional.of(existing));

        String result = tool.execute(Map.of("orderId", 1, "reason", "damaged again"));

        assertTrue(result.contains("alreadyPending"));
        verify(approvalService, never()).createApprovalRequest(any(), anyString(), anyString());
    }

    @Test
    void unknownOrder_isRejected() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        String result = tool.execute(Map.of("orderId", 99, "reason", "damaged"));

        assertTrue(result.contains("not found"));
        verify(approvalService, never()).createApprovalRequest(any(), anyString(), anyString());
    }
}
