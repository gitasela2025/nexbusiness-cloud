package com.nexlora.nexbusiness.service;

import com.nexlora.nexbusiness.entity.PendingApproval;
import com.nexlora.nexbusiness.entity.PendingApproval.ActionType;
import com.nexlora.nexbusiness.entity.PendingApproval.ApprovalStatus;
import com.nexlora.nexbusiness.entity.PosOrder;
import com.nexlora.nexbusiness.entity.ProductInventory;
import com.nexlora.nexbusiness.repository.PendingApprovalRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApprovalServiceTest {

    @Mock private PendingApprovalRepository approvalRepository;
    @Mock private PosOrderRepository orderRepository;
    @Mock private ProductInventoryRepository inventoryRepository;

    @InjectMocks private ApprovalService service;

    private PendingApproval approval(ActionType type, String payload, ApprovalStatus status) {
        PendingApproval a = new PendingApproval();
        a.setId(1L);
        a.setActionType(type);
        a.setPayloadJson(payload);
        a.setStatus(status);
        return a;
    }

    private ProductInventory product(long id, int stock) {
        ProductInventory p = new ProductInventory();
        p.setId(id);
        p.setName("Paper Cups (50pk)");
        p.setSku("SKU-002");
        p.setPrice(new BigDecimal("4.20"));
        p.setStockQty(stock);
        p.setBranchId("COLOMBO");
        return p;
    }

    private PosOrder order(long id, long productId, int qty, PosOrder.OrderStatus status) {
        PosOrder o = new PosOrder();
        o.setId(id);
        o.setUserId(1L);
        o.setProductId(productId);
        o.setQuantity(qty);
        o.setTotalPrice(new BigDecimal("8.40"));
        o.setStatus(status);
        return o;
    }

    @Test
    void approvingRefund_marksOrderRefunded_andRestocks() {
        PendingApproval a = approval(ActionType.REFUND, "{\"orderId\":7}", ApprovalStatus.PENDING);
        PosOrder o = order(7L, 2L, 2, PosOrder.OrderStatus.COMPLETED);
        ProductInventory p = product(2L, 3);
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(a));
        when(orderRepository.findById(7L)).thenReturn(Optional.of(o));
        when(inventoryRepository.findById(2L)).thenReturn(Optional.of(p));

        String result = service.updateApprovalStatus(1L, ApprovalStatus.APPROVED);

        assertEquals(PosOrder.OrderStatus.REFUNDED, o.getStatus());
        assertEquals(5, p.getStockQty());
        assertEquals(ApprovalStatus.APPROVED, a.getStatus());
        assertTrue(result.contains("refunded"));
    }

    @Test
    void approvingPurchaseOrder_addsStock() {
        PendingApproval a = approval(ActionType.PURCHASE_ORDER,
            "{\"productId\":2,\"quantity\":40}", ApprovalStatus.PENDING);
        ProductInventory p = product(2L, 3);
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(a));
        when(inventoryRepository.findById(2L)).thenReturn(Optional.of(p));

        service.updateApprovalStatus(1L, ApprovalStatus.APPROVED);

        assertEquals(43, p.getStockQty());
        assertEquals(ApprovalStatus.APPROVED, a.getStatus());
    }

    @Test
    void rejecting_changesNothingElse() {
        PendingApproval a = approval(ActionType.PURCHASE_ORDER,
            "{\"productId\":2,\"quantity\":40}", ApprovalStatus.PENDING);
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(a));

        service.updateApprovalStatus(1L, ApprovalStatus.REJECTED);

        assertEquals(ApprovalStatus.REJECTED, a.getStatus());
        verifyNoInteractions(orderRepository, inventoryRepository);
    }

    @Test
    void approvingTwice_isBlocked() {
        PendingApproval a = approval(ActionType.PURCHASE_ORDER,
            "{\"productId\":2,\"quantity\":40}", ApprovalStatus.APPROVED);
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(a));

        String result = service.updateApprovalStatus(1L, ApprovalStatus.APPROVED);

        assertTrue(result.contains("already"));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void refundOfNonCompletedOrder_staysPending() {
        PendingApproval a = approval(ActionType.REFUND, "{\"orderId\":7}", ApprovalStatus.PENDING);
        PosOrder o = order(7L, 2L, 2, PosOrder.OrderStatus.REFUNDED);
        when(approvalRepository.findById(1L)).thenReturn(Optional.of(a));
        when(orderRepository.findById(7L)).thenReturn(Optional.of(o));

        String result = service.updateApprovalStatus(1L, ApprovalStatus.APPROVED);

        assertTrue(result.startsWith("Cannot approve"));
        assertEquals(ApprovalStatus.PENDING, a.getStatus());
        verify(approvalRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
    }
}
