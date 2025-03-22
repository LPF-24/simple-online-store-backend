package com.simple_online_store_backend.repository;

import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeopleRepository extends JpaRepository<Person, Integer> {
    Optional<Person> findByUserName(String userName);
    List<Person> findAllByRole(String role);
    Optional<Address> findAddressById(int addressId);
}
