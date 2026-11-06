package com.nexlora.nexbusiness.controller;

import com.nexlora.nexbusiness.service.SecureDocService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SecureDocControllerTest {

    private SecureDocController controllerWith(SecureDocService service) {
        SecureDocController c = new SecureDocController();
        ReflectionTestUtils.setField(c, "secureDocService", service);
        return c;
    }

    private final MockMultipartFile pdf =
            new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());

    @Test
    void userCannotUpload() {
        SecureDocService service = mock(SecureDocService.class);
        ResponseEntity<String> r = controllerWith(service).uploadDocument(pdf, "demo", "PUBLIC", "USER");
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        verify(service, never()).processAndStorePdf(any(), anyString(), anyString());
    }

    @Test
    void adminCanUploadPdf() {
        SecureDocService service = mock(SecureDocService.class);
        when(service.processAndStorePdf(any(), anyString(), anyString())).thenReturn("ok");
        ResponseEntity<String> r = controllerWith(service).uploadDocument(pdf, "demo", "PUBLIC", "ADMIN");
        assertEquals(HttpStatus.OK, r.getStatusCode());
    }

    @Test
    void nonPdfIsRejected() {
        SecureDocService service = mock(SecureDocService.class);
        MockMultipartFile txt = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());
        ResponseEntity<String> r = controllerWith(service).uploadDocument(txt, "demo", "PUBLIC", "ADMIN");
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
    }

    @Test
    void userCannotDelete() {
        SecureDocService service = mock(SecureDocService.class);
        ResponseEntity<String> r = controllerWith(service).delete("DOC-1", "USER");
        assertEquals(HttpStatus.FORBIDDEN, r.getStatusCode());
        verify(service, never()).deleteDocument(anyString());
    }
}
