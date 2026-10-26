package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {
}
