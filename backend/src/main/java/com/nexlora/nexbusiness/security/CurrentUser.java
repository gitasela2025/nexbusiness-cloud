package com.nexlora.nexbusiness.security;

import org.springframework.security.core.Authentication;

public final class CurrentUser {
    private static final ThreadLocal<Long> AGENT_USER = new ThreadLocal<>();

    private CurrentUser() {}

    /** User id from the JWT (the filter stores it as the principal). */
    public static Long id(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    public static void setAgentUser(Long id) { AGENT_USER.set(id); }
    public static void clearAgentUser() { AGENT_USER.remove(); }
    public static Long agentUser() { return AGENT_USER.get(); }
}
