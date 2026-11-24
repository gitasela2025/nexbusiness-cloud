package com.nexlora.nexbusiness.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** One question/answer exchange that came through the gateway. */
@Entity
@Table(name = "conversation_logs")
@Data
@NoArgsConstructor
public class ConversationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(name = "external_id", nullable = false, length = 48)
    private String externalId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage;

    @Column(columnDefinition = "TEXT")
    private String reply;

    @Column(name = "tools_used", length = 500)
    private String toolsUsed;

    @Column(name = "needs_human", nullable = false)
    private boolean needsHuman;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
