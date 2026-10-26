package com.nexlora.nexbusiness.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OmniServeService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String askAgent(String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            // Constructing OpenAI-compatible payload for Groq (Llama-3)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama3-70b-8192");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are OmniServe, an AI business automation agent for NexBusiness Cloud, helping with POS, CRM, and inventory."),
                Map.of("role", "user", "content", userMessage)
            ));
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(groqApiUrl, entity, Map.class);

            // Parse response from Groq
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            return "Error: Received empty response from Groq AI.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to communicate with Groq API: " + e.getMessage();
        }
    }
}
