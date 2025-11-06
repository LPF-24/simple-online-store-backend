package com.simple_online_store_backend.repository;

import com.simple_online_store_backend.entity.PickupLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickupLocationRepository extends JpaRepository<PickupLocation, Integer> {
    List<PickupLocation> findByActiveTrue();

    boolean existsByCityIgnoreCaseAndStreetIgnoreCaseAndHouseNumberIgnoreCase(
            String city, String street, String houseNumber
    );
}
