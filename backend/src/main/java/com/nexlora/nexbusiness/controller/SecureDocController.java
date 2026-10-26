package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.service.SecureDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
