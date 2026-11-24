package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.ChannelIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelIdentityRepository extends JpaRepository<ChannelIdentity, Long> {
    Optional<ChannelIdentity> findByChannelAndExternalId(String channel, String externalId);
}
