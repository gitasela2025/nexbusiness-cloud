package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.PendingApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingApprovalRepository extends JpaRepository<PendingApproval, Long> {
}
