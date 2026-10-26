package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.PosOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosOrderRepository extends JpaRepository<PosOrder, Long> {
}
