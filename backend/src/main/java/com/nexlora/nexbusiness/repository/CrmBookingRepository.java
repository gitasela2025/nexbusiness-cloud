package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.CrmBookingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrmBookingRepository extends JpaRepository<CrmBookingTicket, Long> {

    long countByType(CrmBookingTicket.BookingType type);

    long countByTypeAndUserId(CrmBookingTicket.BookingType type, Long userId);
}
