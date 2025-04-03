package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@PreAuthorize(value = "ROLE_ADMIN")
@RequiredArgsConstructor
public class AdminService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<OrderResponseDTO> findAllOrders() {
        return orderRepository.findAll().stream().map(orderMapper::mapEntityToResponse).toList();
    }
}
