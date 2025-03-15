package com.simple_online_store_backend.repository;

import com.simple_online_store_backend.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Person, Integer> {
    Optional<Person> findByUserName(String userName);
}
