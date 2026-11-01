package com.nexlora.nexbusiness.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.agent.Tool;
import com.nexlora.nexbusiness.agent.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OmniServeService {

    private static final Logger log = LoggerFactory.getLogger(OmniServeService.class);
    private static final int MAX_ITERATIONS = 5;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private ToolRegistry toolRegistry;

    public String askAgent(String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                "You are OmniServe, an AI business agent for NexBusiness Cloud (POS, CRM, inventory). "
                + "Use the available tools to look up real data. Never guess stock quantities. "
                + "If a tool finds nothing, say so clearly. "
                + "If a required detail is missing (product, quantity, booking type or time), ask the user for it. "
                + "Only call placeOrder or bookAppointmentOrTicket when the user clearly asks for it. "
                + "Stock quantities are in units of the product as named, for example one unit of 'Paper Cups (50pk)' is one pack. "
                + "If stock is not enough for an order, tell the user and offer to raise a purchase order with requestPurchaseOrder. "
                + "Before raising a purchase order, call checkStock to confirm stock is really low (at or below the reorder level). "
                + "If stock is healthy, tell the user and do not raise the request. "
                + "For questions about appointments or tickets, call listBookings. "
                + "Refunds and purchase orders are never executed directly: use requestRefundApproval or requestPurchaseOrder, "
                + "then tell the user the request is waiting for Admin approval. Never say a refund or purchase order is done. "
                + "A refund needs an order id and a reason, ask the user if either is missing. "
                + "The current date and time is " + java.time.LocalDateTime.now() + ".\n"
                ));
            messages.add(Map.of("role", "user", "content", userMessage));

            for (int step = 1; step <= MAX_ITERATIONS; step++) {
                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("messages", messages);
                body.put("tools", toolRegistry.definitions());
                body.put("tool_choice", "auto");
                body.put("temperature", 0.2);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                    groqApiUrl, new HttpEntity<>(body, headers), Map.class);

                if (response.getBody() == null) {
                    return "Error: Received empty response from Groq AI.";
                }
                List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices == null || choices.isEmpty()) {
                    return "Error: Received empty response from Groq AI.";
                }

                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) msg.get("tool_calls");

                // AI එකට තවත් tool ඕනේ නැත්නම් මෙය අවසන් පිළිතුරයි
                if (toolCalls == null || toolCalls.isEmpty()) {
                    return (String) msg.get("content");
                }

                // AI එකේ tool_calls message එක history එකට දමන්න
                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", msg.get("content"));
                assistantMsg.put("tool_calls", toolCalls);
                messages.add(assistantMsg);

                // සෑම tool call එකක්ම Java වලින් run කර ප්‍රතිඵලය AI ට දෙන්න
                for (Map<String, Object> call : toolCalls) {
                    Map<String, Object> fn = (Map<String, Object>) call.get("function");
                    String toolName = (String) fn.get("name");
                    String argsJson = (String) fn.get("arguments");

                    String result = runTool(toolName, argsJson);
                    log.info("[AGENT] step {}: {}({}) -> {}", step, toolName, argsJson, result);

                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.get("id"));
                    toolMsg.put("content", result);
                    messages.add(toolMsg);
                }
            }

            return "I could not finish this request within " + MAX_ITERATIONS
                 + " steps. Please try a simpler request.";

        } catch (Exception e) {
            log.error("Agent failed", e);
            return "Failed to communicate with Groq API: " + e.getMessage();
        }
    }

    private String runTool(String toolName, String argsJson) {
        Optional<Tool> tool = toolRegistry.get(toolName);
        if (tool.isEmpty()) {
            return errorJson("Unknown tool: " + toolName);
        }
        try {
            Map<String, Object> args = (argsJson == null || argsJson.isBlank())
                ? new HashMap<>()
                : mapper.readValue(argsJson, new TypeReference<Map<String, Object>>() {});
            // Day 6: tool.get().requiresApproval() නම් pending_approvals එකට යවන්න
            return tool.get().execute(args);
        } catch (Exception e) {
            return errorJson("Tool failed: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        try {
            return mapper.writeValueAsString(Map.of("error", message));
        } catch (Exception e) {
            return "{\"error\":\"unknown\"}";
        }
    }
}
