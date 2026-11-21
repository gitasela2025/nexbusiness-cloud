package com.nexlora.nexbusiness.security;

import org.springframework.security.core.Authentication;

public final class CurrentUser {
    private static final ThreadLocal<Long> AGENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> AGENT_ROLE = new ThreadLocal<>();

    private CurrentUser() {}

    /** User id from the JWT (the filter stores it as the principal). */
    public static Long id(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    public static void setAgentUser(Long id) { AGENT_USER.set(id); }
    public static Long agentUser() { return AGENT_USER.get(); }

    /** Role must come from the JWT, never from the chat message. */
    public static void setAgentRole(String role) {
        if (role == null || role.isBlank()) { AGENT_ROLE.remove(); return; }
        String r = role.trim().toUpperCase();
        AGENT_ROLE.set(r.startsWith("ROLE_") ? r.substring(5) : r);
    }

    /** Fail-closed: no role means the least privileged one. */
    public static String agentRole() {
        String r = AGENT_ROLE.get();
        return r == null ? "CUSTOMER" : r;
    }

    public static void clearAgentUser() {
        AGENT_USER.remove();
        AGENT_ROLE.remove();
    }
}
