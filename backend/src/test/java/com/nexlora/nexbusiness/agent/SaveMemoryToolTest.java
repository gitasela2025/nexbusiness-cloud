package com.nexlora.nexbusiness.agent;

import com.nexlora.nexbusiness.entity.UserMemory;
import com.nexlora.nexbusiness.repository.UserMemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaveMemoryToolTest {

    @Mock private UserMemoryRepository memoryRepository;

    @InjectMocks private SaveMemoryTool tool;

    private UserMemory memory(String fact) {
        UserMemory m = new UserMemory();
        m.setMemoryFact(fact);
        return m;
    }

    @Test
    void newFact_isSaved() {
        when(memoryRepository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of());
        when(memoryRepository.save(any(UserMemory.class))).thenAnswer(i -> i.getArgument(0));

        String result = tool.execute(Map.of("fact", "Works at the Colombo branch"));

        assertTrue(result.contains("\"saved\":true"));
        verify(memoryRepository).save(any(UserMemory.class));
    }

    @Test
    void duplicateFact_isNotSavedAgain() {
        when(memoryRepository.findByUserIdOrderByUpdatedAtDesc(1L))
            .thenReturn(List.of(memory("works at the colombo branch")));

        String result = tool.execute(Map.of("fact", "Works at the Colombo branch"));

        assertTrue(result.contains("alreadyKnown"));
        verify(memoryRepository, never()).save(any());
    }

    @Test
    void blankFact_isRejected() {
        String result = tool.execute(Map.of("fact", "   "));

        assertTrue(result.contains("fact is required"));
        verify(memoryRepository, never()).save(any());
    }
}
