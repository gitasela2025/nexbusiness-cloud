package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.service.OmniServeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/omniserve")
@CrossOrigin(origins = "*")
public class OmniServeController {

    @Autowired
    private OmniServeService omniServeService;

    @PostMapping("/chat")
    public ResponseEntity<String> chatWithAgent(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message cannot be empty");
        }
        
        String aiResponse = omniServeService.askAgent(message);
        return ResponseEntity.ok(aiResponse);
    }
}
