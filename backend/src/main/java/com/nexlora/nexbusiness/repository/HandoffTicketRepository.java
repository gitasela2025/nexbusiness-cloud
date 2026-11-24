package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.HandoffTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HandoffTicketRepository extends JpaRepository<HandoffTicket, Long> {
    Optional<HandoffTicket> findFirstByChannelAndExternalIdAndStatusOrderByIdDesc(
            String channel, String externalId, String status);
}
