package com.nexlora.nexbusiness.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.entity.CrmBookingTicket;
import com.nexlora.nexbusiness.repository.CrmBookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class BookAppointmentTool implements Tool {

    // Day 15 (JWT) හිදී සැබෑ user id එක මෙතැනට ආදේශ කරමු
    private static final Long DEFAULT_USER_ID = 1L;

    @Autowired
    private CrmBookingRepository bookingRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "bookAppointmentOrTicket"; }

    @Override
    public String description() {
        return "Create an APPOINTMENT (needs a date and time) or a SUPPORT_TICKET (no time needed). "
             + "Use only when the user clearly asks to book or to raise a ticket.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "type", Map.of("type", "string",
                    "enum", List.of("APPOINTMENT", "SUPPORT_TICKET"),
                    "description", "APPOINTMENT or SUPPORT_TICKET"),
                "description", Map.of("type", "string",
                    "description", "What the booking or ticket is about"),
                "scheduledTime", Map.of("type", "string",
                    "description", "Local date-time like 2026-11-02T15:00:00. Required for APPOINTMENT")),
            "required", List.of("type", "description"));
    }

    @Override
    public boolean requiresApproval() { return false; }

    @Override
    public String execute(Map<String, Object> args) {
        Object rawType = args.get("type");
        Object rawDesc = args.get("description");
        Object rawTime = args.get("scheduledTime");

        CrmBookingTicket.BookingType type;
        try {
            type = CrmBookingTicket.BookingType.valueOf(String.valueOf(rawType).trim().toUpperCase());
        } catch (Exception e) {
            return error("type must be APPOINTMENT or SUPPORT_TICKET");
        }
        if (rawDesc == null || rawDesc.toString().isBlank()) {
            return error("description is required");
        }

        LocalDateTime when = null;
        if (rawTime != null && !rawTime.toString().isBlank()) {
            String t = rawTime.toString().trim();
            try {
                when = LocalDateTime.parse(t);
            } catch (DateTimeParseException e) {
                try {
                    when = java.time.OffsetDateTime.parse(t).toLocalDateTime();
                } catch (DateTimeParseException e2) {
                    return error("scheduledTime must look like 2026-11-02T15:00:00");
                }
            }
        }
        if (type == CrmBookingTicket.BookingType.APPOINTMENT && when == null) {
            return error("An appointment needs a date and time. Ask the user for it.");
        }

        CrmBookingTicket ticket = new CrmBookingTicket();
        ticket.setUserId(DEFAULT_USER_ID);
        ticket.setType(type);
        ticket.setDescription(rawDesc.toString().trim());
        ticket.setScheduledTime(when);
        ticket.setStatus("OPEN");
        ticket = bookingRepository.save(ticket);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("created", true);
        out.put("id", ticket.getId());
        out.put("type", ticket.getType());
        out.put("description", ticket.getDescription());
        out.put("scheduledTime", ticket.getScheduledTime() == null ? null : ticket.getScheduledTime().toString());
        out.put("status", ticket.getStatus());
        return json(out);
    }

    private String error(String msg) {
        return json(Map.of("created", false, "error", msg));
    }

    private String json(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"could not serialize result\"}"; }
    }
}
