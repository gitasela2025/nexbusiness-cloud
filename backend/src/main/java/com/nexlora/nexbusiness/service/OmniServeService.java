package com.nexlora.nexbusiness.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexlora.nexbusiness.agent.AgentResult;
import com.nexlora.nexbusiness.agent.AgentStep;
import com.nexlora.nexbusiness.agent.Tool;
import com.nexlora.nexbusiness.agent.ToolRegistry;
import com.nexlora.nexbusiness.entity.UserMemory;
import com.nexlora.nexbusiness.repository.UserMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OmniServeService {

    private static final Logger log = LoggerFactory.getLogger(OmniServeService.class);
    private static final int MAX_ITERATIONS = 5;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private FailoverLlmService failoverLlmService;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private UserMemoryRepository memoryRepository;

    public String askAgent(String userMessage) {
        return askAgentWithSteps(userMessage).reply();
    }

    @SuppressWarnings("unchecked")
    public AgentResult askAgentWithSteps(String userMessage) {
        List<AgentStep> steps = new ArrayList<>();
        try {
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
                + "For questions about appointments or tickets, call listBookings. Only pass a non-null string for type (e.g. \"appointment\" or \"ticket\"), or omit type completely if listing all—never pass null. "
                + "Refunds and purchase orders are never executed directly: use requestRefundApproval or requestPurchaseOrder, "
                + "then tell the user the request is waiting for Admin approval. Never say a refund or purchase order is done. "
                + "A refund needs an order id and a reason, ask the user if either is missing. "
                + "When the user states lasting facts or preferences in a message, combine them into a SINGLE saveMemory call (never emit multiple saveMemory calls in the same turn). "
                + "Do not save questions, one-off requests, passwords, API keys or card numbers. "
                + "Use the remembered facts below to personalise answers, and do not ask again for something already remembered. "
                + "The current date and time is " + java.time.LocalDateTime.now() + ".\n"
                + buildMemoryContext()
                ));
            messages.add(Map.of("role", "user", "content", userMessage));

            for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
                Map<String, Object> body = new HashMap<>();
                body.put("messages", messages);
                body.put("tools", toolRegistry.definitions());
                body.put("tool_choice", "auto");
                body.put("temperature", 0.2);

                Map<String, Object> responseBody = failoverLlmService.callChatCompletions(body);
                if (responseBody == null) {
                    return new AgentResult("Error: Received empty response from LLM.", steps);
                }
                List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) responseBody.get("choices");
                if (choices == null || choices.isEmpty()) {
                    return new AgentResult("Error: Received empty response from LLM.", steps);
                }

                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) msg.get("tool_calls");

                if (toolCalls == null || toolCalls.isEmpty()) {
                    Object content = msg.get("content");
                    return new AgentResult(content == null ? "" : content.toString(), steps);
                }

                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", msg.get("content"));
                assistantMsg.put("tool_calls", toolCalls);
                messages.add(assistantMsg);

                for (Map<String, Object> call : toolCalls) {
                    Map<String, Object> fn = (Map<String, Object>) call.get("function");
                    String toolName = (String) fn.get("name");
                    String argsJson = (String) fn.get("arguments");

                    String result = runTool(toolName, argsJson);
                    log.info("[AGENT] step {}: {}({}) -> {}", steps.size() + 1, toolName, argsJson, result);

                    steps.add(new AgentStep(
                        steps.size() + 1,
                        toolName,
                        parseArgs(argsJson),
                        parseResult(result),
                        toolRegistry.get(toolName).map(Tool::requiresApproval).orElse(false)));

                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", call.get("id"));
                    toolMsg.put("content", result);
                    messages.add(toolMsg);
                }
            }

            return new AgentResult("I could not finish this request within " + MAX_ITERATIONS
                 + " steps. Please try a simpler request.", steps);

        } catch (Exception e) {
            log.error("Agent failed", e);
            return new AgentResult("Failed to communicate with LLM provider: " + e.getMessage(), steps);
        }
    }

    private String buildMemoryContext() {
        List<UserMemory> memories = memoryRepository.findByUserIdOrderByUpdatedAtDesc(com.nexlora.nexbusiness.security.CurrentUser.agentUser());
        if (memories.isEmpty()) {
            return "\nREMEMBERED FACTS: none yet.\n";
        }
        StringBuilder sb = new StringBuilder("\nREMEMBERED FACTS ABOUT THE USER:\n");
        memories.stream().limit(20).forEach(m -> sb.append("- ").append(m.getMemoryFact()).append("\n"));
        return sb.toString();
    }

    private String runTool(String toolName, String argsJson) {
        Optional<Tool> tool = toolRegistry.get(toolName);
        if (tool.isEmpty()) {
            return errorJson("Unknown tool: " + toolName);
        }
        try {
            Map<String, Object> args = (argsJson == null || argsJson.isBlank())
                ? new HashMap<>()
                : new HashMap<>(mapper.readValue(argsJson, new TypeReference<Map<String, Object>>() {}));
            args.entrySet().removeIf(e -> e.getValue() == null);
            return tool.get().execute(args);
        } catch (Exception e) {
            log.error("Tool {} failed", toolName, e);
            return errorJson("Tool error: " + e.getMessage());
        }
    }

    private Map<String, Object> parseArgs(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return mapper.readValue(argsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("raw", argsJson);
        }
    }

    private Object parseResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return "";
        }
        try {
            return mapper.readValue(resultJson, Object.class);
        } catch (Exception e) {
            return resultJson;
        }
    }

    private String errorJson(String message) {
        try {
            return mapper.writeValueAsString(Map.of("error", message));
        } catch (Exception e) {
            return "{\"error\":\"" + message + "\"}";
        }
    }
}
