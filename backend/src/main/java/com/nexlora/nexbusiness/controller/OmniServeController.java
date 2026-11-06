package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.service.OmniServeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexlora.nexbusiness.security.CurrentUser;
import org.springframework.security.core.Authentication;

import java.util.Map;

@RestController
@RequestMapping("/api/omniserve")
@CrossOrigin(origins = "*")
public class OmniServeController {

    @Autowired
    private OmniServeService omniServeService;

    @PostMapping("/chat")
    public ResponseEntity<?> chatWithAgent(@RequestBody Map<String, String> request, Authentication auth) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message cannot be empty");
        }

        // { "reply": "...", "steps": [ {step, tool, args, result, needsApproval} ] }
        CurrentUser.setAgentUser(CurrentUser.id(auth));
        try {
            return ResponseEntity.ok(omniServeService.askAgentWithSteps(message));
        } finally {
            CurrentUser.clearAgentUser();
        }
    }
}
