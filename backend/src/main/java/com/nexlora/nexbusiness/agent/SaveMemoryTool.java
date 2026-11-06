package com.nexlora.nexbusiness.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.entity.UserMemory;
import com.nexlora.nexbusiness.repository.UserMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.nexlora.nexbusiness.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class SaveMemoryTool implements Tool {
    private static final int MAX_LEN = 300;

    @Autowired
    private UserMemoryRepository memoryRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "saveMemory"; }

    @Override
    public String description() {
        return "Save ONE lasting fact about the user or their business preferences so it is remembered "
             + "in future chats (for example their branch, role or preferred currency). "
             + "Do not save questions, one-off requests, passwords, API keys or card numbers.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "fact", Map.of("type", "string",
                    "description", "One short sentence, for example: Works at the Colombo branch")),
            "required", List.of("fact"));
    }

    @Override
    public boolean requiresApproval() { return false; }

    @Override
    public String execute(Map<String, Object> args) {
        Object raw = args.get("fact");
        if (raw == null || raw.toString().isBlank()) {
            return json(Map.of("saved", false, "error", "fact is required"));
        }
        Long userId = CurrentUser.agentUser();
        if (userId == null) {
            return json(Map.of("saved", false, "error", "no signed-in user"));
        }
        String fact = raw.toString().trim();
        if (fact.length() > MAX_LEN) {
            return json(Map.of("saved", false, "error", "fact is too long, keep it under " + MAX_LEN + " characters"));
        }

        for (UserMemory m : memoryRepository.findByUserIdOrderByUpdatedAtDesc(userId)) {
            if (m.getMemoryFact() != null && m.getMemoryFact().trim().equalsIgnoreCase(fact)) {
                return json(Map.of("saved", false, "alreadyKnown", true, "fact", fact));
            }
        }

        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setMemoryFact(fact);
        memory.setUpdatedAt(LocalDateTime.now());
        memory = memoryRepository.save(memory);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("saved", true);
        out.put("id", memory.getId());
        out.put("fact", fact);
        return json(out);
    }

    private String json(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { return "{\"error\":\"could not serialize result\"}"; }
    }
}
