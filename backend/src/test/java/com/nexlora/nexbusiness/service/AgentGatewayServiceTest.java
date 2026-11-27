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
import com.nexlora.nexbusiness.service.AgentGatewayService.GatewayResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentGatewayServiceTest {

    @Mock OmniServeService omni;
    @Mock ChannelIdentityRepository identities;
    @Mock UserRepository users;
    @Mock ConversationLogRepository logs;
    @Mock HandoffTicketRepository tickets;
    @Mock PasswordEncoder encoder;

    AgentGatewayService service;

    @BeforeEach
    void setUp() {
        service = new AgentGatewayService(omni, identities, users, logs, tickets, encoder);
        when(encoder.encode(anyString())).thenReturn("hashed");
        when(users.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(tickets.save(any(HandoffTicket.class))).thenAnswer(i -> {
            HandoffTicket t = i.getArgument(0);
            t.setId(7L);
            return t;
        });
        when(identities.findByChannelAndExternalId(anyString(), anyString())).thenReturn(Optional.empty());
        when(tickets.findFirstByChannelAndExternalIdAndStatusOrderByIdDesc(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    private static AgentResult answer(String reply, AgentStep... steps) {
        return new AgentResult(reply, List.of(steps));
    }

    @Test
    void unknownIdentityGetsNewCustomerAccountAndLink() {
        when(omni.askAgentWithSteps(anyString(), anyList())).thenReturn(answer("Hello!"));

        service.handle("telegram", "555", "Nimal", "hi");

        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCap.capture());
        assertEquals(User.Role.CUSTOMER, userCap.getValue().getRole());
        assertEquals("telegram-555@channel.local", userCap.getValue().getEmail());

        ArgumentCaptor<ChannelIdentity> idCap = ArgumentCaptor.forClass(ChannelIdentity.class);
        verify(identities).save(idCap.capture());
        assertEquals("555", idCap.getValue().getExternalId());
        assertEquals(42L, idCap.getValue().getUserId());
    }

    @Test
    void roleIsCappedToCustomerEvenWhenLinkedUserIsAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(User.Role.ADMIN);
        ChannelIdentity link = new ChannelIdentity();
        link.setUserId(1L);
        when(identities.findByChannelAndExternalId("telegram", "999")).thenReturn(Optional.of(link));
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        // The mocked agent reports which role/user it was run with
        when(omni.askAgentWithSteps(anyString(), anyList()))
                .thenAnswer(i -> answer(CurrentUser.agentRole() + "/" + CurrentUser.agentUser()));

        GatewayResult r = service.handle("telegram", "999", "x", "hi");

        assertEquals("CUSTOMER/1", r.reply());
        assertFalse(r.needsHuman());
    }

    @Test
    void normalAnswerNeedsNoHumanAndIsLogged() {
        when(omni.askAgentWithSteps(anyString(), anyList())).thenReturn(answer("We have 43 in stock."));

        GatewayResult r = service.handle("telegram", "1", "A", "How many cups?");

        assertFalse(r.needsHuman());
        assertNull(r.handoffTicketId());
        verify(tickets, never()).save(any());
        verify(logs).save(any());
    }

    @Test
    void humanRequestCreatesTicket() {
        when(omni.askAgentWithSteps(anyString(), anyList())).thenReturn(answer("Sure."));

        GatewayResult r = service.handle("telegram", "1", "A", "I want to talk to a human please");

        assertTrue(r.needsHuman());
        assertEquals("human_requested", r.handoffReason());
        assertEquals(7L, r.handoffTicketId());
        assertTrue(r.reply().startsWith(AgentGatewayService.HUMAN_REPLY_PREFIX));
        assertTrue(r.reply().contains("#7"));
    }

    @Test
    void toolErrorTriggersHandoff() {
        AgentStep bad = new AgentStep(1, "checkStock", Map.of(), Map.of("error", "Tool error: db down"), false);
        when(omni.askAgentWithSteps(anyString(), anyList())).thenReturn(answer("Something went wrong.", bad));

        GatewayResult r = service.handle("telegram", "1", "A", "stock?");

        assertTrue(r.needsHuman());
        assertEquals("tool_error", r.handoffReason());
        assertEquals(List.of("checkStock"), r.toolsUsed());
    }

    @Test
    void validationErrorDoesNotTriggerHandoff() {
        AgentStep notFound = new AgentStep(1, "requestRefundApproval", Map.of(),
                Map.of("requested", false, "error", "Order 3 not found"), false);
        when(omni.askAgentWithSteps(anyString(), anyList())).thenReturn(answer("I couldn't find that order.", notFound));

        GatewayResult r = service.handle("telegram", "1", "A", "refund order 3");

        assertFalse(r.needsHuman());
        assertNull(r.handoffTicketId());
        verify(tickets, never()).save(any());
    }

    @Test
    void unsureAnswerTriggersHandoff() {
        when(omni.askAgentWithSteps(anyString(), anyList())).thenReturn(answer("That is not mentioned in the documents."));

        GatewayResult r = service.handle("telegram", "1", "A", "refund policy?");

        assertTrue(r.needsHuman());
        assertEquals("unsure_answer", r.handoffReason());
    }

    @Test
    void agentCrashGivesSafeReplyAndTicket() {
        when(omni.askAgentWithSteps(anyString(), anyList())).thenThrow(new RuntimeException("groq down"));

        GatewayResult r = service.handle("telegram", "1", "A", "hello");

        assertTrue(r.needsHuman());
        assertEquals("agent_error", r.handoffReason());
        assertEquals(AgentGatewayService.SAFE_REPLY, r.reply());
        assertNotNull(r.handoffTicketId());
    }

    @Test
    void recentConversationIsPassedToTheAgentOldestFirst() {
        ConversationLog newer = new ConversationLog();
        newer.setUserMessage("because they were damaged");
        newer.setReply("Which order ID?");
        ConversationLog older = new ConversationLog();
        older.setUserMessage("refund please");
        older.setReply("What is the reason?");
        when(logs.findTop6ByChannelAndExternalIdOrderByIdDesc("telegram", "1"))
                .thenReturn(List.of(newer, older));
        List<java.util.Map<String, String>> seen = new ArrayList<>();
        when(omni.askAgentWithSteps(anyString(), anyList())).thenAnswer(i -> {
            seen.addAll(i.getArgument(1));
            return answer("ok");
        });

        service.handle("telegram", "1", "A", "order 999");

        assertEquals(4, seen.size());
        assertEquals("user", seen.get(0).get("role"));
        assertEquals("refund please", seen.get(0).get("content"));
        assertEquals("assistant", seen.get(3).get("role"));
        assertEquals("Which order ID?", seen.get(3).get("content"));
    }

    @Test
    void oldConversationIsNotPassedToTheAgent() {
        ConversationLog stale = new ConversationLog();
        stale.setUserMessage("old question");
        stale.setReply("old answer");
        stale.setCreatedAt(LocalDateTime.now().minusHours(2));
        when(logs.findTop6ByChannelAndExternalIdOrderByIdDesc("telegram", "1"))
                .thenReturn(List.of(stale));
        List<java.util.Map<String, String>> seen = new ArrayList<>();
        when(omni.askAgentWithSteps(anyString(), anyList())).thenAnswer(i -> {
            seen.addAll(i.getArgument(1));
            return answer("ok");
        });

        service.handle("telegram", "1", "A", "hello");

        assertTrue(seen.isEmpty());
    }

    @Test
    void llmProviderFailureTextIsHiddenFromCustomer() {
        when(omni.askAgentWithSteps(anyString(), anyList()))
                .thenReturn(answer("Failed to communicate with LLM provider: Groq LLM call failed"));

        GatewayResult r = service.handle("telegram", "1", "A", "hello");

        assertEquals(AgentGatewayService.SAFE_REPLY, r.reply());
        assertTrue(r.needsHuman());
        assertEquals("agent_error", r.handoffReason());
    }

    @Test
    void humanRequestReplyHidesAgentOwnTicketNumber() {
        when(omni.askAgentWithSteps(anyString(), anyList()))
                .thenReturn(answer("Logged as support ticket #6."));

        GatewayResult r = service.handle("telegram", "1", "A", "I want to talk to a human");

        assertTrue(r.needsHuman());
        assertFalse(r.reply().contains("#6"));
    }

    @Test
    void humanRequestReplyShowsOnlyHandoffTicketNumber() {
        when(omni.askAgentWithSteps(anyString(), anyList())).thenReturn(answer("Sure."));

        GatewayResult r = service.handle("telegram", "1", "A", "I want to talk to a human");

        assertTrue(r.reply().startsWith(AgentGatewayService.HUMAN_REPLY_PREFIX));
    }

    @Test
    void openTicketIsReusedNotDuplicated() {
        HandoffTicket open = new HandoffTicket();
        open.setId(3L);
        when(tickets.save(any(HandoffTicket.class))).thenAnswer(i -> i.getArgument(0));
        when(tickets.findFirstByChannelAndExternalIdAndStatusOrderByIdDesc("telegram", "1", HandoffTicket.OPEN))
                .thenReturn(Optional.of(open));
        when(omni.askAgentWithSteps(anyString(), anyList())).thenReturn(answer("ok"));

        service.handle("telegram", "1", "A", "I need a manager");

        ArgumentCaptor<HandoffTicket> cap = ArgumentCaptor.forClass(HandoffTicket.class);
        verify(tickets).save(cap.capture());
        assertEquals(3L, cap.getValue().getId());
    }
}
