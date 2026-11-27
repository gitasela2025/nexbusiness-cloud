package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.ConversationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationLogRepository extends JpaRepository<ConversationLog, Long> {
    List<ConversationLog> findTop6ByChannelAndExternalIdOrderByIdDesc(String channel, String externalId);
}
