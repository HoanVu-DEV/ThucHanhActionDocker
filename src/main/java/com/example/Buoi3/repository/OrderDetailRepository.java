package com.example.Buoi3.repository;

import com.example.Buoi3.Entity.OrderDetail; 
import org.springframework.data.jpa.repository.JpaRepository; 
import org.springframework.stereotype.Repository; 
@Repository 
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> { 
}
