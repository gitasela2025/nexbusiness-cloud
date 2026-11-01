package com.nexlora.nexbusiness.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.entity.PendingApproval;
import com.nexlora.nexbusiness.entity.ProductInventory;
import com.nexlora.nexbusiness.repository.ProductInventoryRepository;
import com.nexlora.nexbusiness.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RequestPurchaseOrderTool implements Tool {

    private static final String SESSION = "omniserve-agent";
    private static final int MAX_QTY = 10000;

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ProductInventoryRepository inventoryRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "requestPurchaseOrder"; }

    @Override
    public String description() {
        return "Ask an Admin to approve a purchase order to restock a product from the supplier. "
             + "This does NOT buy anything: it only creates a PENDING request. After calling it, tell the "
             + "user the purchase order is waiting for Admin approval. Quantity is in units of the product "
             + "as named (for example one unit of 'Paper Cups (50pk)' is one pack).";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "productName", Map.of("type", "string", "description", "Product to restock"),
                "quantity", Map.of("type", "integer", "description", "Units to purchase (at least 1)"),
                "branch", Map.of("type", "string", "description", "Optional branch, for example COLOMBO"),
                "reason", Map.of("type", "string", "description", "Optional reason, for example low stock")),
            "required", List.of("productName", "quantity"));
    }

    @Override
    public boolean requiresApproval() { return true; }

    @Override
    public String execute(Map<String, Object> args) {
        Object rawName = args.get("productName");
        Integer qty = toInt(args.get("quantity"));
        String branch = args.get("branch") == null ? null : args.get("branch").toString().trim();
        String reason = args.get("reason") == null ? "" : args.get("reason").toString().trim();

        if (rawName == null || rawName.toString().isBlank()) {
            return error("productName is required.");
        }
        if (qty == null || qty < 1 || qty > MAX_QTY) {
            return error("quantity must be a whole number between 1 and " + MAX_QTY);
        }

        List<ProductInventory> matches = new ArrayList<>(
            inventoryRepository.findByNameContainingIgnoreCase(rawName.toString().trim()));
        if (branch != null && !branch.isEmpty()) {
            matches.removeIf(p -> !p.getBranchId().equalsIgnoreCase(branch));
        }
        if (matches.isEmpty()) {
            return error("No product matches that name.");
        }
        if (matches.size() > 1) {
            List<String> options = new ArrayList<>();
            for (ProductInventory p : matches) {
                options.add(p.getName() + " @ " + p.getBranchId());
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("requested", false);
            out.put("error", "Several products match. Ask the user which one they mean.");
            out.put("matches", options);
            return json(out);
        }

        ProductInventory product = matches.get(0);

        Optional<PendingApproval> existing = approvalService.findPendingDuplicate(
            PendingApproval.ActionType.PURCHASE_ORDER, "productId", product.getId());
        if (existing.isPresent()) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("requested", false);
            out.put("alreadyPending", true);
            out.put("approvalId", existing.get().getId());
            out.put("message", "A purchase order for this product is already waiting for Admin approval.");
            return json(out);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", product.getId());
        payload.put("productName", product.getName());
        payload.put("branch", product.getBranchId());
        payload.put("quantity", qty);
        payload.put("currentStock", product.getStockQty());
        payload.put("reason", reason);

        PendingApproval saved = approvalService.createApprovalRequest(
            PendingApproval.ActionType.PURCHASE_ORDER, json(payload), SESSION);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("requested", true);
        out.put("approvalId", saved.getId());
        out.put("status", "PENDING");
        out.put("message", "Purchase order request created. NOT ordered yet: an Admin must approve it first.");
        return json(out);
    }

    private Integer toInt(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue() == n.intValue() ? n.intValue() : null;
        }
        if (o instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
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
