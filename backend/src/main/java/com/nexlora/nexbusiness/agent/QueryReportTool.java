package com.nexlora.nexbusiness.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.bi.ReadOnlyDb;
import com.nexlora.nexbusiness.bi.SqlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QueryReportTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(QueryReportTool.class);

    private final ReadOnlyDb db;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile String schemaText;

    public QueryReportTool(ReadOnlyDb db) { this.db = db; }

    @Override public String name() { return "queryReport"; }

    @Override
    public String description() {
        return "Run a read-only MySQL SELECT for business reports (sales by branch, stock levels, "
             + "bookings). Write one SELECT statement using only these tables: " + schema()
             + ". MySQL syntax, no comments, no semicolons. Max " + SqlValidator.MAX_ROWS
             + " rows. Returns rows as JSON.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "sql", Map.of("type", "string", "description", "A single MySQL SELECT statement")),
            "required", List.of("sql"));
    }

    @Override public boolean requiresApproval() { return false; }

    @Override public Set<String> allowedRoles() { return Set.of("ADMIN"); }

    @Override
    public String execute(Map<String, Object> args) {
        Object raw = args.get("sql");
        String sql;
        try {
            sql = SqlValidator.validate(raw == null ? null : raw.toString());
        } catch (IllegalArgumentException e) {
            log.warn("queryReport rejected: {} | sql={}", e.getMessage(), raw);
            return err(e.getMessage());
        }

        log.info("queryReport run: {}", sql);
        try {
            List<Map<String, Object>> rows = db.jdbc().queryForList(sql);
            boolean truncated = rows.size() > SqlValidator.MAX_ROWS;
            if (truncated) rows = rows.subList(0, SqlValidator.MAX_ROWS);

            List<Map<String, Object>> clean = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                Map<String, Object> o = new LinkedHashMap<>();
                r.forEach((k, v) -> o.put(k, (v == null || v instanceof Number
                        || v instanceof Boolean || v instanceof String) ? v : v.toString()));
                clean.add(o);
            }

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("rowCount", clean.size());
            out.put("truncated", truncated);
            out.put("rows", clean);
            return mapper.writeValueAsString(out);
        } catch (JsonProcessingException e) {
            return err("could not serialize result");
        } catch (Exception e) {
            log.warn("queryReport failed: {}", e.getMessage());
            return err("Query failed. Check column names and syntax, then try again.");
        }
    }

    private String schema() {
        if (schemaText != null) return schemaText;
        try {
            List<Map<String, Object>> cols = db.jdbc().queryForList(
                "SELECT table_name AS t, column_name AS c, data_type AS d "
              + "FROM information_schema.columns WHERE table_schema = DATABASE() "
              + "AND table_name IN ('pos_orders','products_inventory','crm_bookings_tickets') "
              + "ORDER BY table_name, ordinal_position");
            if (cols.isEmpty()) return "pos_orders, products_inventory, crm_bookings_tickets";

            Map<String, List<String>> byTable = new LinkedHashMap<>();
            for (Map<String, Object> r : cols) {
                byTable.computeIfAbsent(String.valueOf(r.get("t")), k -> new ArrayList<>())
                       .add(r.get("c") + " " + r.get("d"));
            }
            StringBuilder sb = new StringBuilder();
            byTable.forEach((t, c) -> sb.append(t).append('(').append(String.join(", ", c)).append(") "));
            schemaText = sb.toString().trim();
            return schemaText;
        } catch (Exception e) {
            return "pos_orders, products_inventory, crm_bookings_tickets";
        }
    }

    private String err(String msg) {
        try { return mapper.writeValueAsString(Map.of("error", msg)); }
        catch (JsonProcessingException e) { return "{\"error\":\"query rejected\"}"; }
    }
}
