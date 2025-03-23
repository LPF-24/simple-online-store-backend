package com.simple_online_store_backend.repository;

import com.simple_online_store_backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    Optional<Address> findByCityAndStreetAndHouseNumberAndApartment(String city, String street, String houseNumber, String apartment);
}
