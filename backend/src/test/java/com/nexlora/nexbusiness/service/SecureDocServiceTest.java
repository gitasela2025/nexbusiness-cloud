package com.nexlora.nexbusiness.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecureDocServiceTest {

    @Test
    void shortTextIsOneChunk() {
        assertEquals(1, SecureDocService.split("hello world").size());
    }

    @Test
    void emptyTextGivesNoChunks() {
        assertTrue(SecureDocService.split("").isEmpty());
    }

    @Test
    void longTextIsSplitWithOverlap() {
        String text = "a".repeat(2000);
        List<String> pieces = SecureDocService.split(text);
        assertTrue(pieces.size() > 2);
        assertTrue(pieces.stream().allMatch(p -> p.length() <= 800));
    }
}
