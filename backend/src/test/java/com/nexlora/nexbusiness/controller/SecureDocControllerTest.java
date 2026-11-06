package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.service.SecureDocService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecureDocControllerTest {

    private SecureDocController controllerWith(SecureDocService service) {
        SecureDocController c = new SecureDocController();
        ReflectionTestUtils.setField(c, "secureDocService", service);
        return c;
    }

    private Authentication as(String role) {
        return new UsernamePasswordAuthenticationToken(1L, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private final MockMultipartFile pdf =
            new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

    @Test
    void customerCannotUpload() {
        SecureDocService service = mock(SecureDocService.class);
        ResponseEntity<String> r = controllerWith(service).uploadDocument(pdf, "demo", "PUBLIC", as("CUSTOMER"));
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        verify(service, never()).processAndStorePdf(any(), anyString(), anyString());
    }

    @Test
    void adminCanUploadPdf() {
        SecureDocService service = mock(SecureDocService.class);
        when(service.processAndStorePdf(any(), anyString(), anyString())).thenReturn("ok");
        ResponseEntity<String> r = controllerWith(service).uploadDocument(pdf, "demo", "PUBLIC", as("ADMIN"));
        assertEquals(HttpStatus.OK, r.getStatusCode());
    }

    @Test
    void nonPdfIsRejected() {
        SecureDocService service = mock(SecureDocService.class);
        MockMultipartFile txt = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());
        ResponseEntity<String> r = controllerWith(service).uploadDocument(txt, "demo", "PUBLIC", as("ADMIN"));
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void customerCannotDelete() {
        SecureDocService service = mock(SecureDocService.class);
        ResponseEntity<String> r = controllerWith(service).delete("DOC-1", as("CUSTOMER"));
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        verify(service, never()).deleteDocument(anyString());
    }

    @Test
    void customerAskIsNotAdmin() {
        SecureDocService service = mock(SecureDocService.class);
        controllerWith(service).ask(java.util.Map.of("question", "q"), as("CUSTOMER"));
        verify(service).ask("demo", "q", false);
    }
}
