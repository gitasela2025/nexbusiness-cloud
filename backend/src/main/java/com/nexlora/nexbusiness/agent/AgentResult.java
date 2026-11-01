package com.nexlora.nexbusiness.agent;

import java.util.List;

/** Final answer plus the tool calls that led to it. */
public record AgentResult(String reply, List<AgentStep> steps) {
}
