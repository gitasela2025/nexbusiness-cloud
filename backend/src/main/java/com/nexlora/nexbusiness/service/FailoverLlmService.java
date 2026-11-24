package com.nexlora.nexbusiness.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "llm")
class LlmProperties {

    private ProviderConfig primary = new ProviderConfig();
    private ProviderConfig fallback = new ProviderConfig();

    public ProviderConfig getPrimary() {
        return primary;
    }

    public void setPrimary(ProviderConfig primary) {
        this.primary = primary;
    }

    public ProviderConfig getFallback() {
        return fallback;
    }

    public void setFallback(ProviderConfig fallback) {
        this.fallback = fallback;
    }

    public static class ProviderConfig {
        private String provider = "groq";
        private String baseUrl = "https://api.groq.com/openai/v1/chat/completions";
        private String apiKey;
        private String model = "llama-3.3-70b-versatile";
        private int timeoutMs = 8000;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }
}

@Service
public class FailoverLlmService {

    private static final Logger log = LoggerFactory.getLogger(FailoverLlmService.class);
    private final LlmProperties llmProperties;

    public FailoverLlmService(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
    }

    public Map<String, Object> callChatCompletions(Map<String, Object> requestBody) {
        LlmProperties.ProviderConfig primary = llmProperties.getPrimary();
        LlmProperties.ProviderConfig fallback = llmProperties.getFallback();

        try {
            return invokeProvider(primary, requestBody);
        } catch (Exception primaryEx) {
            log.warn("Primary Groq model [{}] failed: {}",
                    primary.getModel(), primaryEx.getMessage());

            if (fallback == null || fallback.getModel() == null || fallback.getModel().isBlank()
                    || fallback.getModel().equals(primary.getModel())) {
                throw new RuntimeException("Groq LLM call failed", primaryEx);
            }

            log.info("Retrying with secondary Groq model [{}]", fallback.getModel());
            return invokeProvider(fallback, requestBody);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> invokeProvider(LlmProperties.ProviderConfig cfg, Map<String, Object> baseRequestBody) {
        String apiKey = (cfg != null && cfg.getApiKey() != null && !cfg.getApiKey().isBlank())
                ? cfg.getApiKey().trim()
                : (llmProperties.getPrimary() != null ? llmProperties.getPrimary().getApiKey() : null);

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is missing");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(cfg.getTimeoutMs());
        factory.setReadTimeout(cfg.getTimeoutMs());
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        Map<String, Object> payload = new HashMap<>(baseRequestBody);
        payload.put("model", cfg.getModel());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(cfg.getBaseUrl(), entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response body from Groq");
            }
            return responseBody;
        } catch (RestClientException ex) {
            throw new RuntimeException("HTTP/network error calling Groq: " + ex.getMessage(), ex);
        }
    }
}
