package com.simple_online_store_backend.repository;

import com.simple_online_store_backend.entity.PickupLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickupLocationRepository extends JpaRepository<PickupLocation, Integer> {
}
