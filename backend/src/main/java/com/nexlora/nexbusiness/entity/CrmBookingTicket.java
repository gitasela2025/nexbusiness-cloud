package com.nexlora.nexbusiness.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_bookings_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrmBookingTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Column(length = 50)
    private String status = "OPEN";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum BookingType {
        APPOINTMENT, SUPPORT_TICKET
    }
}
