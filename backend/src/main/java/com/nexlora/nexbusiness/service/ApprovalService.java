package com.nexlora.nexbusiness.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.entity.PendingApproval;
import com.nexlora.nexbusiness.entity.PosOrder;
import com.nexlora.nexbusiness.entity.ProductInventory;
import com.nexlora.nexbusiness.repository.PendingApprovalRepository;
import com.nexlora.nexbusiness.repository.PosOrderRepository;
import com.nexlora.nexbusiness.repository.ProductInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ApprovalService {

    @Autowired
    private PendingApprovalRepository approvalRepository;

    @Autowired
    private PosOrderRepository orderRepository;

    @Autowired
    private ProductInventoryRepository inventoryRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    // Create a pending action request (triggered by AI agent)
    public PendingApproval createApprovalRequest(PendingApproval.ActionType actionType, String payloadJson, String session) {
        PendingApproval approval = new PendingApproval();
        approval.setActionType(actionType);
        approval.setPayloadJson(payloadJson);
        approval.setRequestedByAiSession(session);
        approval.setStatus(PendingApproval.ApprovalStatus.PENDING);

        return approvalRepository.save(approval);
    }

    // Returns every request (the UI shows PENDING / APPROVED / REJECTED badges)
    public List<PendingApproval> getPendingApprovals() {
        return approvalRepository.findAll();
    }

    // AI retry වලින් duplicate ඉල්ලීම් හැදීම වළක්වයි
    public Optional<PendingApproval> findPendingDuplicate(PendingApproval.ActionType type, String field, long value) {
        for (PendingApproval a : approvalRepository.findAll()) {
            if (a.getActionType() == type && a.getStatus() == PendingApproval.ApprovalStatus.PENDING) {
                try {
                    if (mapper.readTree(a.getPayloadJson()).path(field).asLong(-1) == value) {
                        return Optional.of(a);
                    }
                } catch (Exception ignored) {
                    // unreadable payload: skip it
                }
            }
        }
        return Optional.empty();
    }

    // Admin approves or rejects. APPROVED නම් සැබෑ ක්‍රියාව මෙතනදී පමණක් සිදු වේ.
    @Transactional
    public String updateApprovalStatus(Long id, PendingApproval.ApprovalStatus status) {
        Optional<PendingApproval> optionalApproval = approvalRepository.findById(id);
        if (optionalApproval.isEmpty()) {
            return "Approval request not found.";
        }
        PendingApproval approval = optionalApproval.get();

        if (approval.getStatus() != PendingApproval.ApprovalStatus.PENDING) {
            return "Approval request " + id + " was already " + approval.getStatus() + " and cannot be changed.";
        }
        if (status == PendingApproval.ApprovalStatus.PENDING) {
            return "Status must be APPROVED or REJECTED.";
        }

        String outcome = "";
        if (status == PendingApproval.ApprovalStatus.APPROVED) {
            try {
                outcome = executeAction(approval);
            } catch (Exception e) {
                // ක්‍රියාව අසාර්ථක නම් request එක PENDING ලෙසම තබයි
                return "Cannot approve request " + id + ": " + e.getMessage();
            }
        }

        approval.setStatus(status);
        approvalRepository.save(approval);
        return "Approval request " + id + " has been updated to " + status
             + (outcome.isEmpty() ? "" : ". " + outcome);
    }

    private String executeAction(PendingApproval approval) throws Exception {
        JsonNode p = mapper.readTree(approval.getPayloadJson());
        switch (approval.getActionType()) {
            case REFUND:
                return executeRefund(p);
            case PURCHASE_ORDER:
                return executePurchaseOrder(p);
            default:
                throw new IllegalStateException("Unknown action type");
        }
    }

    private String executeRefund(JsonNode p) {
        long orderId = p.path("orderId").asLong(-1);
        PosOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalStateException("order " + orderId + " not found"));
        if (order.getStatus() != PosOrder.OrderStatus.COMPLETED) {
            throw new IllegalStateException("order " + orderId + " is " + order.getStatus() + ", not COMPLETED");
        }
        ProductInventory product = inventoryRepository.findById(order.getProductId())
            .orElseThrow(() -> new IllegalStateException("product of the order not found"));

        // validation සම්පූර්ණයි, දැන් පමණක් වෙනස් කරන්න
        order.setStatus(PosOrder.OrderStatus.REFUNDED);
        orderRepository.save(order);
        product.setStockQty(product.getStockQty() + order.getQuantity());
        inventoryRepository.save(product);
        return "Order " + orderId + " refunded, " + order.getQuantity() + " unit(s) returned to stock.";
    }

    private String executePurchaseOrder(JsonNode p) {
        long productId = p.path("productId").asLong(-1);
        int qty = p.path("quantity").asInt(0);
        if (qty < 1) {
            throw new IllegalStateException("invalid quantity");
        }
        ProductInventory product = inventoryRepository.findById(productId)
            .orElseThrow(() -> new IllegalStateException("product " + productId + " not found"));

        // සරල කළ demo: අනුමත PO එකෙන් භාණ්ඩ ලැබුණා ලෙස stock එකතු කරයි
        product.setStockQty(product.getStockQty() + qty);
        inventoryRepository.save(product);
        return "Purchase order approved: " + qty + " unit(s) of " + product.getName()
             + " added, stock is now " + product.getStockQty() + ".";
    }
}
