package com.nexlora.nexbusiness.repository;

import com.nexlora.nexbusiness.entity.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {
    List<ProductInventory> findByNameContainingIgnoreCase(String name);
}
