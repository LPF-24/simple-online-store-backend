package com.simple_online_store_backend.repository;

import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByPerson(Person person);
    List<Order> findByPersonAndStatus(Person person, OrderStatus status);
    Boolean existsByPerson_Id(Integer id);
    @EntityGraph(attributePaths = {"products", "person", "address", "pickupLocation"})
    Optional<Order> findWithDetailsById(Integer id);

    List<Order> findByPerson_Id(Integer personId);
}
