package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {
    List<UserMemory> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
