package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.service.AgentGatewayService;
import com.nexlora.nexbusiness.service.AgentGatewayService.GatewayResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * POST /api/agent/chat, protected by X-API-Key (see ApiKeyFilter).
 * Body: { "channel": "telegram", "externalId": "123456", "name": "Nimal", "message": "..." }
 * Any "role" field in the body is ignored on purpose.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentGatewayController {

    private static final Pattern CHANNEL = Pattern.compile("^[a-z][a-z0-9_]{1,19}$");
    private static final Pattern EXTERNAL_ID = Pattern.compile("^[A-Za-z0-9_.:@-]{1,48}$");
    private static final int MAX_MESSAGE = 4000;

    private final AgentGatewayService gateway;

    public AgentGatewayController(AgentGatewayService gateway) {
        this.gateway = gateway;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body) {
        String channel = text(body.get("channel"));
        String externalId = text(body.get("externalId"));
        String name = text(body.get("name"));
        String message = text(body.get("message"));

        if (!CHANNEL.matcher(channel).matches()) {
            return bad("channel must be 2-20 chars: lowercase letters, digits, underscore");
        }
        if (!EXTERNAL_ID.matcher(externalId).matches()) {
            return bad("externalId is required (max 48 chars)");
        }
        if (message.isBlank()) {
            return bad("message cannot be empty");
        }
        if (message.length() > MAX_MESSAGE) {
            return bad("message is too long (max " + MAX_MESSAGE + " chars)");
        }
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }

        GatewayResult r = gateway.handle(channel, externalId, name, message);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reply", r.reply());
        out.put("needs_human", r.needsHuman());
        out.put("handoff_reason", r.handoffReason());
        out.put("handoff_ticket_id", r.handoffTicketId());
        out.put("tools_used", r.toolsUsed());
        return ResponseEntity.ok(out);
    }

    private static String text(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static ResponseEntity<Map<String, String>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }
}
