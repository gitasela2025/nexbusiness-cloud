package com.nexlora.nexbusiness.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.entity.PendingApproval;
import com.nexlora.nexbusiness.entity.PosOrder;
import com.nexlora.nexbusiness.repository.PosOrderRepository;
import com.nexlora.nexbusiness.repository.ProductInventoryRepository;
import com.nexlora.nexbusiness.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RequestRefundApprovalTool implements Tool {

    private static final String SESSION = "omniserve-agent";

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private PosOrderRepository orderRepository;

    @Autowired
    private ProductInventoryRepository inventoryRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "requestRefundApproval"; }

    @Override
    public String description() {
        return "Ask an Admin to approve a refund for a completed order. This does NOT refund anything: "
             + "it only creates a PENDING request. After calling it, tell the user the refund is waiting "
             + "for Admin approval. Needs the order id and a reason.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "orderId", Map.of("type", "integer", "description", "Id of the order to refund"),
                "reason", Map.of("type", "string", "description", "Why the refund is needed")),
            "required", List.of("orderId", "reason"));
    }

    @Override
    public boolean requiresApproval() { return true; }

    @Override
    public String execute(Map<String, Object> args) {
        Long orderId = toLong(args.get("orderId"));
        Object rawReason = args.get("reason");

        if (orderId == null || orderId < 1) {
            return error("orderId is required. Ask the user for the order id.");
        }
        if (rawReason == null || rawReason.toString().isBlank()) {
            return error("reason is required. Ask the user why the refund is needed.");
        }

        PosOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return error("Order " + orderId + " was not found.");
        }
        if (order.getStatus() == PosOrder.OrderStatus.REFUNDED) {
            return error("Order " + orderId + " is already refunded.");
        }
        if (order.getStatus() != PosOrder.OrderStatus.COMPLETED) {
            return error("Order " + orderId + " is " + order.getStatus() + " and cannot be refunded.");
        }

        Optional<PendingApproval> existing =
            approvalService.findPendingDuplicate(PendingApproval.ActionType.REFUND, "orderId", orderId);
        if (existing.isPresent()) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("requested", false);
            out.put("alreadyPending", true);
            out.put("approvalId", existing.get().getId());
            out.put("message", "A refund request for this order is already waiting for Admin approval.");
            return json(out);
        }

        String productName = inventoryRepository.findById(order.getProductId())
            .map(p -> p.getName()).orElse("unknown product");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("productName", productName);
        payload.put("quantity", order.getQuantity());
        payload.put("amount", order.getTotalPrice());
        payload.put("reason", rawReason.toString().trim());

        PendingApproval saved = approvalService.createApprovalRequest(
            PendingApproval.ActionType.REFUND, json(payload), SESSION);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("requested", true);
        out.put("approvalId", saved.getId());
        out.put("status", "PENDING");
        out.put("message", "Refund request created. NOT refunded yet: an Admin must approve it first.");
        return json(out);
    }

    private Long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private String error(String msg) {
        return json(Map.of("requested", false, "error", msg));
    }

    private String json(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"could not serialize result\"}"; }
    }
}
