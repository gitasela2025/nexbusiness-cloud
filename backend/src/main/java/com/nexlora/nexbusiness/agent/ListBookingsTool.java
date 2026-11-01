package com.nexlora.nexbusiness.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.entity.CrmBookingTicket;
import com.nexlora.nexbusiness.repository.CrmBookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ListBookingsTool implements Tool {

    private static final int LIMIT = 20;

    @Autowired
    private CrmBookingRepository bookingRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "listBookings"; }

    @Override
    public String description() {
        return "List existing appointments and support tickets (latest 20). "
             + "Optionally filter by type. Use it to answer questions about bookings "
             + "or to check for a clash before creating a new appointment.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "type", Map.of("type", "string",
                    "enum", List.of("APPOINTMENT", "SUPPORT_TICKET"),
                    "description", "Optional filter")),
            "required", List.of());
    }

    @Override
    public boolean requiresApproval() { return false; }

    @Override
    public String execute(Map<String, Object> args) {
        String filter = args.get("type") == null ? null : args.get("type").toString().trim().toUpperCase();

        List<CrmBookingTicket> all = new ArrayList<>(bookingRepository.findAll());
        all.sort(Comparator.comparing(CrmBookingTicket::getId).reversed());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CrmBookingTicket b : all) {
            if (filter != null && !filter.isEmpty() && !b.getType().name().equals(filter)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("type", b.getType().name());
            row.put("description", b.getDescription());
            row.put("scheduledTime", b.getScheduledTime() == null ? null : b.getScheduledTime().toString());
            row.put("status", b.getStatus());
            rows.add(row);
            if (rows.size() >= LIMIT) break;
        }

        try {
            return mapper.writeValueAsString(Map.of("count", rows.size(), "bookings", rows));
        } catch (Exception e) {
            return "{\"error\":\"could not serialize result\"}";
        }
    }
}
