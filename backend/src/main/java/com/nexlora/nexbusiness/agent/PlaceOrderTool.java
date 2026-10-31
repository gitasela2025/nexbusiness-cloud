package com.nexlora.nexbusiness.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.entity.PosOrder;
import com.nexlora.nexbusiness.entity.ProductInventory;
import com.nexlora.nexbusiness.repository.PosOrderRepository;
import com.nexlora.nexbusiness.repository.ProductInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
public class PlaceOrderTool implements Tool {

    // Day 15 (JWT) හිදී සැබෑ user id එක මෙතැනට ආදේශ කරමු
    private static final Long DEFAULT_USER_ID = 1L;
    private static final int MAX_QTY = 10000;

    @Autowired
    private ProductInventoryRepository inventoryRepository;

    @Autowired
    private PosOrderRepository orderRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "placeOrder"; }

    @Override
    public String description() {
        return "Place a sales order for a product. Checks stock first, saves the order and "
             + "reduces stock. Use only when the user clearly asks to order or buy something. "
             + "If stock is not enough, the order is NOT placed and the available quantity is returned.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "productName", Map.of("type", "string",
                    "description", "Product name, for example Paper Cups"),
                "quantity", Map.of("type", "integer",
                    "description", "Number of units to order (at least 1)"),
                "branch", Map.of("type", "string",
                    "description", "Optional branch, for example COLOMBO. Use it when several branches stock the product")),
            "required", List.of("productName", "quantity"));
    }

    @Override
    public boolean requiresApproval() { return false; }

    @Override
    @Transactional
    public String execute(Map<String, Object> args) {
        Object rawName = args.get("productName");
        Integer qty = toInt(args.get("quantity"));
        String branch = args.get("branch") == null ? null : args.get("branch").toString().trim();

        if (rawName == null || rawName.toString().isBlank()) {
            return error("productName is required");
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
            return error("No product matches that name");
        }
        if (matches.size() > 1) {
            List<String> options = new ArrayList<>();
            for (ProductInventory p : matches) {
                options.add(p.getName() + " @ " + p.getBranchId());
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("placed", false);
            out.put("error", "Several products match. Ask the user which one they mean.");
            out.put("matches", options);
            return json(out);
        }

        ProductInventory product = matches.get(0);
        if (product.getStockQty() < qty) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("placed", false);
            out.put("error", "Not enough stock");
            out.put("product", product.getName());
            out.put("requested", qty);
            out.put("available", product.getStockQty());
            return json(out);
        }

        product.setStockQty(product.getStockQty() - qty);
        inventoryRepository.save(product);

        PosOrder order = new PosOrder();
        order.setUserId(DEFAULT_USER_ID);
        order.setProductId(product.getId());
        order.setQuantity(qty);
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        order.setStatus(PosOrder.OrderStatus.COMPLETED);
        order = orderRepository.save(order);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("placed", true);
        out.put("orderId", order.getId());
        out.put("product", product.getName());
        out.put("branch", product.getBranchId());
        out.put("quantity", qty);
        out.put("totalPrice", order.getTotalPrice());
        out.put("remainingStock", product.getStockQty());
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
        return json(Map.of("placed", false, "error", msg));
    }

    private String json(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"could not serialize result\"}"; }
    }
}
