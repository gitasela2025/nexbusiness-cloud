package com.nexlora.nexbusiness.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A conversation the AI could not (or should not) finish alone. A human picks it up. */
@Entity
@Table(name = "handoff_tickets")
@Data
@NoArgsConstructor
public class HandoffTicket {

    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(name = "external_id", nullable = false, length = 48)
    private String externalId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(nullable = false, length = 10)
    private String status = OPEN;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
