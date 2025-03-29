package com.simple_online_store_backend.mapper;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.entity.Order;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapper {
    private final ModelMapper modelMapper;

    public Order mapRequestToOrder(OrderRequestDTO dto) {
        return modelMapper.map(dto, Order.class);
    }

    public OrderResponseDTO mapEntityToResponse(Order order) {
        return modelMapper.map(order, OrderResponseDTO.class);
    }
}
