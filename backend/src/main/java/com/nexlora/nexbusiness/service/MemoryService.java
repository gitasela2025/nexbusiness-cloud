package com.nexlora.nexbusiness.service;

import com.nexlora.nexbusiness.entity.UserMemory;
import com.nexlora.nexbusiness.repository.UserMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemoryService {

    @Autowired
    private UserMemoryRepository memoryRepository;

    // Save a long-term fact for a user
    public UserMemory saveMemory(Long userId, String fact) {
        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setMemoryFact(fact);
        return memoryRepository.save(memory);
    }

    // Retrieve all memories for a specific user to inject into AI context
    public List<UserMemory> getUserMemories(Long userId) {
        return memoryRepository.findAll().stream()
                .filter(m -> m.getUserId().equals(userId))
                .toList();
    }
}
