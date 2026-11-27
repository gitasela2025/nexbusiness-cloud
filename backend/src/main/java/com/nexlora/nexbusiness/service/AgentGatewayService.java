package com.nexlora.nexbusiness.service;

import com.nexlora.nexbusiness.agent.AgentResult;
import com.nexlora.nexbusiness.agent.AgentStep;
import com.nexlora.nexbusiness.entity.ChannelIdentity;
import com.nexlora.nexbusiness.entity.ConversationLog;
import com.nexlora.nexbusiness.entity.HandoffTicket;
import com.nexlora.nexbusiness.entity.User;
import com.nexlora.nexbusiness.repository.ChannelIdentityRepository;
import com.nexlora.nexbusiness.repository.ConversationLogRepository;
import com.nexlora.nexbusiness.repository.HandoffTicketRepository;
import com.nexlora.nexbusiness.repository.UserRepository;
import com.nexlora.nexbusiness.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Entry point for external channels (Telegram, n8n, web widgets).
 * Identity comes from channel_identities, never from the request. The agent always runs
 * with the CUSTOMER role here, even if the linked user is an admin in the database.
 */
@Service
public class AgentGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AgentGatewayService.class);

    /** Channels can never act with more than this role. */
    public static final String CAPPED_ROLE = "CUSTOMER";

    static final String SAFE_REPLY =
            "Sorry, I can't answer that right now. A team member will follow up with you.";
    static final String HUMAN_REPLY_PREFIX =
            "I've passed your request to a human team member. Your ticket number is #";
    static final String HANDOFF_NOTICE =
            "\n\nI've flagged this conversation for a human team member.";

    /** Only recent exchanges count as the same conversation. */
    private static final long HISTORY_WINDOW_MINUTES = 30;

    private static final Pattern HUMAN_REQUEST = Pattern.compile(
            "\\b(human|real person|representative|manager|operator|customer service|complain\\w*"
                    + "|speak to someone|talk to someone)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> UNSURE_PHRASES = List.of(
            "not mentioned", "i'm not sure", "i am not sure", "need a human");

    public record GatewayResult(String reply, boolean needsHuman, String handoffReason,
                                Long handoffTicketId, List<String> toolsUsed) {
    }

    private final OmniServeService omniServeService;
    private final ChannelIdentityRepository identities;
    private final UserRepository users;
    private final ConversationLogRepository logs;
    private final HandoffTicketRepository tickets;
    private final PasswordEncoder passwordEncoder;

    public AgentGatewayService(OmniServeService omniServeService,
                               ChannelIdentityRepository identities,
                               UserRepository users,
                               ConversationLogRepository logs,
                               HandoffTicketRepository tickets,
                               PasswordEncoder passwordEncoder) {
        this.omniServeService = omniServeService;
        this.identities = identities;
        this.users = users;
        this.logs = logs;
        this.tickets = tickets;
        this.passwordEncoder = passwordEncoder;
    }

    public GatewayResult handle(String channel, String externalId, String displayName, String message) {
        User user = resolveUser(channel, externalId, displayName);

        List<Map<String, String>> history = loadHistory(channel, externalId);
        AgentResult result = null;
        String reason = null;
        CurrentUser.setAgentUser(user.getId());
        CurrentUser.setAgentRole(CAPPED_ROLE);
        try {
            result = omniServeService.askAgentWithSteps(message, history);
        } catch (Exception e) {
            log.error("Agent failed for {}:{}", channel, externalId, e);
            reason = "agent_error";
        } finally {
            CurrentUser.clearAgentUser();
        }

        // The LLM layer reports its own failures as plain reply text. Never show that to customers.
        if (result != null && isProviderFailure(result.reply())) {
            log.error("LLM provider failure for {}:{}: {}", channel, externalId, result.reply());
            reason = "agent_error";
            result = null;
        }

        List<AgentStep> steps = (result == null || result.steps() == null) ? List.of() : result.steps();
        String reply = result == null || result.reply() == null ? SAFE_REPLY : result.reply();

        if (reason == null) {
            reason = detectHandoffReason(message, reply, steps);
        }

        Long ticketId = null;
        if (reason != null) {
            ticketId = openOrReuseTicket(channel, externalId, user.getId(), reason, message);
            if ("human_requested".equals(reason) && ticketId != null) {
                // One clear message and one ticket number, not the agent's own CRM ticket text.
                reply = HUMAN_REPLY_PREFIX + ticketId + ". We will get back to you shortly.";
            } else if (!"agent_error".equals(reason)) {
                reply = reply + HANDOFF_NOTICE;
            }
        }

        List<String> tools = steps.stream().map(AgentStep::tool).toList();
        saveLog(channel, externalId, user.getId(), message, reply, tools, reason != null);

        return new GatewayResult(reply, reason != null, reason, ticketId, tools);
    }

    static boolean isProviderFailure(String reply) {
        return reply != null && (reply.startsWith("Failed to communicate with LLM provider")
                || reply.startsWith("Error: Received empty response from LLM"));
    }

    /** Returns why a human should step in, or null when the AI answer can stand. */
    static String detectHandoffReason(String message, String reply, List<AgentStep> steps) {
        if (message != null && HUMAN_REQUEST.matcher(message).find()) {
            return "human_requested";
        }
        for (AgentStep s : steps) {
            // Only real failures (an exception inside a tool) need a human.
            // Normal validation errors such as "order not found" stay in the conversation.
            if (s.result() instanceof Map<?, ?> m
                    && m.get("error") instanceof String err
                    && err.startsWith("Tool error")) {
                return "tool_error";
            }
        }
        String lower = reply == null ? "" : reply.toLowerCase();
        for (String phrase : UNSURE_PHRASES) {
            if (lower.contains(phrase)) {
                return "unsure_answer";
            }
        }
        return null;
    }

    /** Last few exchanges from this chat (oldest first), so the agent remembers the conversation. */
    private List<Map<String, String>> loadHistory(String channel, String externalId) {
        List<ConversationLog> recent = logs.findTop6ByChannelAndExternalIdOrderByIdDesc(channel, externalId);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(HISTORY_WINDOW_MINUTES);
        List<ConversationLog> fresh = new ArrayList<>();
        for (ConversationLog l : recent) {
            if (l.getCreatedAt() != null && l.getCreatedAt().isBefore(cutoff)) {
                break;
            }
            fresh.add(l);
        }
        Collections.reverse(fresh);
        List<Map<String, String>> history = new ArrayList<>();
        for (ConversationLog l : fresh) {
            history.add(Map.of("role", "user", "content", clip(l.getUserMessage())));
            String reply = l.getReply() == null ? "" : l.getReply().replace(HANDOFF_NOTICE, "");
            history.add(Map.of("role", "assistant", "content", clip(reply)));
        }
        return history;
    }

    private static String clip(String s) {
        if (s == null) return "";
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }

    private User resolveUser(String channel, String externalId, String displayName) {
        Optional<ChannelIdentity> existing = identities.findByChannelAndExternalId(channel, externalId);
        if (existing.isPresent()) {
            Optional<User> linked = users.findById(existing.get().getUserId());
            if (linked.isPresent()) {
                return linked.get();
            }
        }

        // First message from this person: create a low-privilege account that cannot log in.
        User u = new User();
        u.setName(displayName == null || displayName.isBlank() ? channel + " user" : displayName);
        u.setEmail(channel + "-" + externalId + "@channel.local");
        u.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        u.setRole(User.Role.CUSTOMER);
        User saved = users.save(u);

        ChannelIdentity identity = existing.orElseGet(ChannelIdentity::new);
        identity.setChannel(channel);
        identity.setExternalId(externalId);
        identity.setUserId(saved.getId());
        identities.save(identity);
        return saved;
    }

    private Long openOrReuseTicket(String channel, String externalId, Long userId,
                                   String reason, String message) {
        Optional<HandoffTicket> open = tickets
                .findFirstByChannelAndExternalIdAndStatusOrderByIdDesc(channel, externalId, HandoffTicket.OPEN);
        HandoffTicket t = open.orElseGet(HandoffTicket::new);
        t.setChannel(channel);
        t.setExternalId(externalId);
        t.setUserId(userId);
        t.setReason(reason);
        t.setLastMessage(message);
        return tickets.save(t).getId();
    }

    private void saveLog(String channel, String externalId, Long userId, String message,
                         String reply, List<String> tools, boolean needsHuman) {
        ConversationLog l = new ConversationLog();
        l.setChannel(channel);
        l.setExternalId(externalId);
        l.setUserId(userId);
        l.setUserMessage(message);
        l.setReply(reply);
        String joined = String.join(",", tools);
        l.setToolsUsed(joined.length() > 500 ? joined.substring(0, 500) : joined);
        l.setNeedsHuman(needsHuman);
        logs.save(l);
    }
}
