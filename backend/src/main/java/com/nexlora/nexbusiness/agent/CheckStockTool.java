package com.nexlora.nexbusiness.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.entity.ProductInventory;
import com.nexlora.nexbusiness.repository.ProductInventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CheckStockTool implements Tool {

    @Autowired
    private ProductInventoryRepository inventoryRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "checkStock"; }

    @Override
    public String description() {
        return "Check current stock for a product by name (partial match allowed). "
             + "Returns SKU, branch, quantity in stock, price and reorder level.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "productName", Map.of(
                    "type", "string",
                    "description", "Product name, for example Paper Cups")),
            "required", List.of("productName"));
    }

    @Override
    public boolean requiresApproval() { return false; }

    @Override
    public String execute(Map<String, Object> args) {
        Object raw = args.get("productName");
        if (raw == null || raw.toString().isBlank()) {
            return "{\"error\":\"productName is required\"}";
        }

        List<ProductInventory> found =
            inventoryRepository.findByNameContainingIgnoreCase(raw.toString().trim());
        if (found.isEmpty()) {
            return "{\"found\":false,\"message\":\"No product matches that name\"}";
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProductInventory p : found) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", p.getName());
            row.put("sku", p.getSku());
            row.put("branch", p.getBranchId());
            row.put("stockQty", p.getStockQty());
            row.put("reorderLevel", p.getReorderLevel());
            row.put("price", p.getPrice());
            row.put("lowStock", p.getStockQty() <= p.getReorderLevel());
            rows.add(row);
        }

        try {
            return mapper.writeValueAsString(Map.of("found", true, "products", rows));
        } catch (JsonProcessingException e) {
            return "{\"error\":\"could not serialize result\"}";
        }
    }
}
