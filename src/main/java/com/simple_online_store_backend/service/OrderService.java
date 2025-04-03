package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.repository.AddressRepository;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.repository.ProductRepository;
import com.simple_online_store_backend.security.PersonDetails;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

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

        List<Product> products = dto.getProductsIds().stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new ValidationException("Product with ID " + id + " not found")))
                .toList();

        if (products.stream().anyMatch(p -> !Boolean.TRUE.equals(p.getAvailability()))) {
            throw new ValidationException("Some products are not available for order");
        }

        Order order = orderMapper.mapRequestToOrder(dto);
        if (dto.getAddress() != null) {
            addressRepository.save(dto.getAddress());
            order.setAddress(dto.getAddress());
        }

        order.setPerson(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setProducts(products);

        Order savedOrder = orderRepository.save(order);
        logger.info("Id of saved order: {}", savedOrder.getId());
        return orderMapper.mapEntityToResponse(savedOrder);
    }

    @PreAuthorize("isAuthenticated()")
    public OrderResponseDTO getOrderById(int orderId) {
        Order foundOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with ID " + orderId + " not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        String role = personDetails.getAuthorities().stream()
                .findFirst().map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        if (!role.equals("ROLE_ADMIN")) {
            if (foundOrder.getPerson() == null || !foundOrder.getPerson().getId().equals(personDetails.getId())) {
                throw new AccessDeniedException("You are not authorized to view this order");
            }
        }

        return orderMapper.mapEntityToResponse(foundOrder);
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
