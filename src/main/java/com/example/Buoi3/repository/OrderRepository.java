package com.example.Buoi3.repository;
import com.example.Buoi3.Entity.Order; 

import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
@Repository 
public interface OrderRepository extends JpaRepository<Order, Long> { 
} 