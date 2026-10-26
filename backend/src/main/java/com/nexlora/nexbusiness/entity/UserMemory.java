package com.nexlora.nexbusiness.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_memories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMemory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "memory_fact", nullable = false, columnDefinition = "TEXT")
    private String memoryFact;

    @Column(name = "extracted_at", updatable = false)
    private LocalDateTime extractedAt = LocalDateTime.now();
}
