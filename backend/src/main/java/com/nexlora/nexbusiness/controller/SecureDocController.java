package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.service.SecureDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/securedoc")
@CrossOrigin(origins = "*")
public class SecureDocController {

    @Autowired
    private SecureDocService secureDocService;

    static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("namespace") String namespace,
            @RequestParam("accessRole") String accessRole,
            Authentication auth) {

        if (!isAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can upload documents.");
        }
        String name = file.getOriginalFilename();
        if (file.isEmpty() || name == null || !name.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("Please upload a non-empty PDF file.");
        }
        return ResponseEntity.ok(secureDocService.processAndStorePdf(file, namespace, accessRole));
    }

    @PostMapping("/ask")
    public ResponseEntity<SecureDocService.AskResult> ask(
            @RequestBody Map<String, String> body, Authentication auth) {
        String namespace = body.getOrDefault("namespace", "demo");
        String question = body.getOrDefault("question", "");
        return ResponseEntity.ok(secureDocService.ask(namespace, question, isAdmin(auth)));
    }

    @GetMapping("/documents")
    public ResponseEntity<List<SecureDocService.DocInfo>> list(
            @RequestParam(value = "namespace", defaultValue = "demo") String namespace,
            Authentication auth) {
        return ResponseEntity.ok(secureDocService.listDocuments(namespace, isAdmin(auth)));
    }

    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<String> delete(@PathVariable String docId, Authentication auth) {
        if (!isAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can delete documents.");
        }
        return secureDocService.deleteDocument(docId)
                ? ResponseEntity.ok("Deleted " + docId)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Document not found.");
    }
}
