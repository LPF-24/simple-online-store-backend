package com.simple_online_store_backend.repository;

import com.simple_online_store_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findAllByAvailabilityTrue();
    List<Product> findByProductName(String name);
}
