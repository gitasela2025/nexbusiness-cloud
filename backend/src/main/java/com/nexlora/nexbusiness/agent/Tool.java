package com.nexlora.nexbusiness.agent;

import java.util.Map;
import java.util.Set;

public interface Tool {
    String name();                              // e.g. "checkStock"
    String description();                       // tells the AI what this tool does
    Map<String, Object> parametersSchema();     // JSON schema of the arguments
    boolean requiresApproval();                 // true = Admin must approve first
    String execute(Map<String, Object> args);   // the real work; result goes back to the AI

    /** Roles that may see and run this tool. Default: everyone (existing tools unchanged). */
    default Set<String> allowedRoles() {
        return Set.of("CUSTOMER", "CASHIER", "ADMIN");
    }
}
