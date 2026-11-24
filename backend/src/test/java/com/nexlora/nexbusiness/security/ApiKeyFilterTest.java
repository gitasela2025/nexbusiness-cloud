package com.nexlora.nexbusiness.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ApiKeyFilterTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse call(ApiKeyFilter f, String path, String key, FilterChain chain) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        if (key != null) req.addHeader("X-API-Key", key);
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, chain);
        return res;
    }

    @Test
    void wrongKeyIsRejected() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse res = call(new ApiKeyFilter("secret"), "/api/agent/chat", "nope", chain);

        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void missingKeyIsRejected() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse res = call(new ApiKeyFilter("secret"), "/api/agent/chat", null, chain);

        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void blankConfiguredKeyRejectsEverything() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse res = call(new ApiKeyFilter(""), "/api/agent/chat", "", chain);

        assertEquals(401, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void correctKeyGrantsGatewayRole() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse res = call(new ApiKeyFilter("secret"), "/api/agent/chat", "secret", chain);

        assertEquals(200, res.getStatus());
        verify(chain).doFilter(any(), any());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GATEWAY")));
    }

    @Test
    void otherPathsAreNotTouched() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        call(new ApiKeyFilter("secret"), "/api/omniserve/chat", null, chain);

        verify(chain).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
