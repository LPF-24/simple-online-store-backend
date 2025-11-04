package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.order.*;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.repository.*;
import com.simple_online_store_backend.security.PersonDetails;
import jakarta.persistence.EntityNotFoundException;
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
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PeopleRepository peopleRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final PickupLocationRepository pickupLocationRepository;
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper, PeopleRepository peopleRepository, ProductRepository productRepository, AddressRepository addressRepository, PickupLocationRepository pickupLocationRepository) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.peopleRepository = peopleRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.pickupLocationRepository = pickupLocationRepository;
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional
    public OrderDetailsResponse createOrder(OrderCreateRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var pd = (PersonDetails) auth.getPrincipal();

        var owner = peopleRepository.findByUserName(pd.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (Boolean.TRUE.equals(owner.getDeleted())) {
            throw new ValidationException("Your account is deactivated. Please restore your account before placing an order.");
        }

        // 1) Продукты
        var products = req.getProductIds().stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Product with ID " + id + " not found")))
                .toList();

        if (products.isEmpty()) {
            throw new ValidationException("You must add at least one product");
        }
        if (products.stream().anyMatch(p -> !Boolean.TRUE.equals(p.getAvailability()))) {
            throw new ValidationException("Some products are not available for order");
        }

        // 2) Доставка: адрес или ПВЗ
        var hasAddress = req.getAddressId() != null;
        var hasPickup  = req.getPickupLocationId() != null;
        if (hasAddress == hasPickup) {
            throw new ValidationException("Either addressId or pickupLocationId must be provided (but not both)");
        }

        var order = new Order();
        order.setPerson(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setProducts(new java.util.ArrayList<>(products));

        if (hasAddress) {
            var address = addressRepository.findById(req.getAddressId())
                    .orElseThrow(() -> new EntityNotFoundException("Address not found: " + req.getAddressId()));
            // при желании: проверить принадлежность адреса пользователю
            order.setAddress(address);
        } else {
            var pickup = pickupLocationRepository.findById(req.getPickupLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("Pickup location not found: " + req.getPickupLocationId()));
            if (!Boolean.TRUE.equals(pickup.getActive())) {
                throw new ValidationException("Pickup location must be active");
            }
            order.setPickupLocation(pickup);
        }

        var saved = orderRepository.save(order);
        return orderMapper.toDetails(saved);
    }

    @PreAuthorize("isAuthenticated()")
    public OrderDetailsResponse getOrderById(int orderId) {
        Order foundOrder = orderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getPrincipal() instanceof PersonDetails pd)) {
            throw new AccessDeniedException("You are not authorized to view this order");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            Integer ownerId = (foundOrder.getPerson() != null ? foundOrder.getPerson().getId() : null);
            if (ownerId == null || !ownerId.equals(pd.getId())) {
                throw new AccessDeniedException("You are not authorized to view this order");
            }
        }

        return orderMapper.toDetails(foundOrder);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @Transactional(readOnly = true)
    public List<OrderListItemResponse> findAllOrdersByCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails pd = (PersonDetails) auth.getPrincipal();
        Integer userId = pd.getId();

        List<Order> orders = orderRepository.findByPerson_Id(userId);
        return orders.stream()
                .map(orderMapper::toListItem).toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public OrderResponseDTO cancelOrder(int orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with ID " + orderId + " not found"));

        if (!order.getPerson().getId().equals(personDetails.getId())) {
            throw new AccessDeniedException(("You are not authorized to cancel this order"));
        }

        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new ValidationException("Only orders with status PENDING can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return orderMapper.mapEntityToResponse(order);
    }

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public OrderResponseDTO reactivateOrder(int orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with ID " + orderId + " not found"));

        if (!order.getPerson().getId().equals(personDetails.getId())) {
            throw new AccessDeniedException(("You are not authorized to reactivate this order"));
        }

        if (!order.getStatus().equals(OrderStatus.CANCELLED)) {
            throw new ValidationException("Only orders with status CANCELLED can be reactivated");
        }

        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        return orderMapper.mapEntityToResponse(order);
    }
}
