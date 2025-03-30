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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PeopleRepository peopleRepository;

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        Person owner = peopleRepository.findByUserName(personDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверка активности аккаунта
        if (owner.getIsDeleted()) {
            throw new ValidationException("Your account is deactivated. Please restore your account before placing an order.");
        }

        Order order = orderMapper.mapRequestToOrder(dto);
        order.setPerson(owner);
        order.setStatus(OrderStatus.PENDING);

        orderRepository.save(order);
        return orderMapper.mapEntityToResponse(order);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    public List<OrderResponseDTO> findAllOrdersByCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int customerId = ((PersonDetails) authentication.getPrincipal()).getId();

        Person customer = peopleRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer wasn't found"));

        return orderRepository.findByPerson(customer).stream().map(orderMapper::mapEntityToResponse).toList();
    }

    /*@Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public OrderResponseDTO */
}
