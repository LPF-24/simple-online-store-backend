package com.simple_online_store_backend.unit.service;

import com.simple_online_store_backend.dto.order.OrderListItemResponse;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.service.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {

    @Mock OrderRepository orderRepository;
    @Mock OrderMapper orderMapper;

    @InjectMocks AdminService adminService;

    @Test
    void findAllOrders_returnsMappedList_inSameOrder() {
        // Arrange
        Order o1 = new Order(); o1.setId(1); o1.setStatus(OrderStatus.PENDING);
        Order o2 = new Order(); o2.setId(2); o2.setStatus(OrderStatus.PROCESSING);

        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        OrderListItemResponse r1 = new OrderListItemResponse();
        r1.setId(1); r1.setStatus(OrderStatus.PENDING); r1.setProductCount(3);

        OrderListItemResponse r2 = new OrderListItemResponse();
        r2.setId(2); r2.setStatus(OrderStatus.PROCESSING); r2.setProductCount(0);

        when(orderMapper.toListItem(o1)).thenReturn(r1);
        when(orderMapper.toListItem(o2)).thenReturn(r2);

        // Act
        List<OrderListItemResponse> result = adminService.findAllOrders();

        // Assert
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals(OrderStatus.PENDING, result.get(0).getStatus());
        assertEquals(3, result.get(0).getProductCount());

        assertEquals(2, result.get(1).getId());
        assertEquals(OrderStatus.PROCESSING, result.get(1).getStatus());
        assertEquals(0, result.get(1).getProductCount());

        verify(orderRepository).findAll();
        verify(orderMapper).toListItem(o1);
        verify(orderMapper).toListItem(o2);
        verifyNoMoreInteractions(orderRepository, orderMapper);
    }

    @Test
    void findAllOrders_returnsEmptyList_whenRepositoryEmpty() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<OrderListItemResponse> result = adminService.findAllOrders();

        assertEquals(0, result.size());
        verify(orderRepository).findAll();
        verifyNoInteractions(orderMapper);
    }
}

