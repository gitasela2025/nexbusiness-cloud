package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.service.SecureDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/securedoc")
@CrossOrigin(origins = "*")
public class SecureDocController {

    @Autowired
    private SecureDocService secureDocService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("namespace") String namespace,
            @RequestParam("accessRole") String accessRole) {

        String result = secureDocService.processAndStorePdf(file, namespace, accessRole);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ask")
    public ResponseEntity<SecureDocService.AskResult> ask(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Demo-Role", defaultValue = "USER") String role) {

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        String namespace = body.getOrDefault("namespace", "demo");
        String question = body.getOrDefault("question", "");
        return ResponseEntity.ok(secureDocService.ask(namespace, question, isAdmin));
    }
}
