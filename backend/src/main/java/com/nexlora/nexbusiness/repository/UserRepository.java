package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom query methods (if needed later) can be added here
}
