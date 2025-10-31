package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.repository.OrderRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@PreAuthorize(value = "ROLE_ADMIN")
public class AdminService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public AdminService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<OrderResponseDTO> findAllOrders() {
        return orderRepository.findAll().stream().map(orderMapper::mapEntityToResponse).toList();
    }
}
