package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.security.PersonDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PeopleRepository peopleRepository;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {
        System.out.println("Method createOrder in Service started");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        Person owner = peopleRepository.findByUserName(personDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("User received");

        // 🔥 Проверка активности аккаунта
        if (owner.getIsDeleted()) {
            throw new ValidationException("Your account is deactivated. Please restore your account before placing an order.");
        }

        Order order = orderMapper.mapRequestToOrder(dto);
        System.out.println("OrderRequestDTO mapped to Order (Entity)");
        order.setPerson(owner);
        order.setStatus(OrderStatus.PENDING);
        System.out.println("Additional order information has been set");

        orderRepository.save(order);
        System.out.println("Order saved");

        System.out.println("Method createOrder in Service ended");
        return orderMapper.mapEntityToResponse(order);
    }
}
