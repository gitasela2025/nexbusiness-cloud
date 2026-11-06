package com.nexlora.nexbusiness.service;

import com.nexlora.nexbusiness.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecureDocAskTest {

    @Test
    void userRoleIsPassedToRepositoryAsNonAdmin() {
        DocumentChunkRepository repo = mock(DocumentChunkRepository.class);
        when(repo.search(anyString(), anyString(), anyBoolean())).thenReturn(new ArrayList<>());
        SecureDocService service = new SecureDocService();
        ReflectionTestUtils.setField(service, "chunkRepository", repo);

        SecureDocService.AskResult result = service.ask("demo", "salary?", false);

        verify(repo).search("demo", "salary?", false);
        verify(repo, never()).search(anyString(), anyString(), eq(true));
        assertTrue(result.sources().isEmpty());
        assertTrue(result.answer().contains("could not find"));
    }
}
