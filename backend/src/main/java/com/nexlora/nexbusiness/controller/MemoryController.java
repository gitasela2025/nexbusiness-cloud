package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.entity.UserMemory;
import com.nexlora.nexbusiness.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memories")
@CrossOrigin(origins = "*")
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserMemory>> getMemories(@PathVariable Long userId) {
        return ResponseEntity.ok(memoryService.getUserMemories(userId));
    }

    @PostMapping("/save")
    public ResponseEntity<UserMemory> saveMemory(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String fact = (String) request.get("fact");

        UserMemory saved = memoryService.saveMemory(userId, fact);
        return ResponseEntity.ok(saved);
    }
}
