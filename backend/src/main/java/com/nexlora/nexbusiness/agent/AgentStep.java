package com.nexlora.nexbusiness.agent;

import java.util.Map;

/** One tool call made by the agent, shown as a timeline step in the UI. */
public record AgentStep(
    int step,
    String tool,
    Map<String, Object> args,
    Object result,
    boolean needsApproval) {
}
