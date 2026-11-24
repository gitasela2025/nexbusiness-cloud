package com.nexlora.nexbusiness.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Maps an external chat identity (e.g. a Telegram user id) to an internal user. */
@Entity
@Table(name = "channel_identities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"channel", "external_id"}))
@Data
@NoArgsConstructor
public class ChannelIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(name = "external_id", nullable = false, length = 48)
    private String externalId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
