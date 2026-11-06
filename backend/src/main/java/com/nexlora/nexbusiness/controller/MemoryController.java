package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.entity.UserMemory;
import com.nexlora.nexbusiness.repository.UserMemoryRepository;
import com.nexlora.nexbusiness.security.CurrentUser;
import com.nexlora.nexbusiness.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memories")
@CrossOrigin(origins = "*")
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private UserMemoryRepository memoryRepository;

    // The URL userId is ignored: users only ever see their own memories.
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserMemory>> getMemories(@PathVariable Long userId, Authentication auth) {
        return ResponseEntity.ok(memoryService.getUserMemories(CurrentUser.id(auth)));
    }

    @PostMapping("/save")
    public ResponseEntity<UserMemory> saveMemory(@RequestBody Map<String, Object> request, Authentication auth) {
        String fact = (String) request.get("fact");
        return ResponseEntity.ok(memoryService.saveMemory(CurrentUser.id(auth), fact));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMemory(@PathVariable Long id, Authentication auth) {
        UserMemory m = memoryRepository.findById(id).orElse(null);
        if (m == null || !m.getUserId().equals(CurrentUser.id(auth))) {
            return ResponseEntity.status(404).body("Memory not found");
        }
        memoryRepository.deleteById(id);
        return ResponseEntity.ok("Memory " + id + " deleted");
    }
}
