package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.entity.*;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.repository.*;
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
    private final PickupLocationRepository pickupLocationRepository;
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

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public OrderResponseDTO updateOrder(int orderId, OrderRequestDTO dto) {
        logger.info("Starting updateOrder for orderId = {}", orderId);

        Order orderToUpdate = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.error("Order with ID {} not found", orderId);
                    return new EntityNotFoundException("Order with ID " + orderId + " not found");
                });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();

        Person currentUser = peopleRepository.findById(personDetails.getId())
                .orElseThrow(() -> {
                    logger.error("User with ID {} not found", personDetails.getId());
                    return new EntityNotFoundException("User not found");
                });

        boolean isAdmin = personDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !orderToUpdate.getPerson().getId().equals(currentUser.getId())) {
            logger.warn("Unauthorized access attempt to order {} by user {}", orderId, currentUser.getId());
            throw new AccessDeniedException("You are not authorized to update this order");
        }

        logger.debug("User {} authorized to update order {}", currentUser.getId(), orderId);

        if (!orderToUpdate.getStatus().equals(OrderStatus.PENDING)) {
            logger.warn("Attempt to update order {} with status {}", orderId, orderToUpdate.getStatus());
            throw new ValidationException("Only orders with status PENDING can be updated");
        }

        if (currentUser.getIsDeleted()) {
            logger.warn("Deactivated user {} attempted to update order", currentUser.getId());
            throw new ValidationException("Your account is deactivated. Please restore your account before updating an order.");
        }

        logger.debug("Loading products for update...");
        List<Product> products = dto.getProductsIds().stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> {
                            logger.error("Product with ID {} not found", id);
                            return new ValidationException("Product with ID " + id + " not found");
                        }))
                .toList();

        if (products.stream().anyMatch(p -> !Boolean.TRUE.equals(p.getAvailability()))) {
            logger.warn("Some products are unavailable for order {}", orderId);
            throw new ValidationException("Some products are not available for order");
        }

        orderToUpdate.setProducts(products);

        if (dto.getAddress() != null) {
            logger.debug("Updating address for order {}", orderId);
            Address address = dto.getAddress();
            List<Address> matchingAddresses = addressRepository.findAllByCityAndStreetAndHouseNumberAndApartment(
                    address.getCity(), address.getStreet(), address.getHouseNumber(), address.getApartment()
            );

            Address addressToSet;

            if (!matchingAddresses.isEmpty()) {
                if (matchingAddresses.size() > 1) {
                    logger.warn("Multiple addresses found for {} {} {} apt {}",
                            dto.getAddress().getCity(),
                            dto.getAddress().getStreet(),
                            dto.getAddress().getHouseNumber(),
                            dto.getAddress().getApartment());
                }
                addressToSet = matchingAddresses.get(0); // используем первый
            } else {
                addressToSet = addressRepository.save(dto.getAddress());
                logger.info("Saved new address with id = {}", addressToSet.getId());
            }

            orderToUpdate.setAddress(addressToSet);
            orderToUpdate.setPickupLocation(null);
        } else if (dto.getPickupLocation() != null) {
            logger.debug("Updating pickup location for order {}", orderId);
            PickupLocation location = dto.getPickupLocation();
            PickupLocation locationToSet = pickupLocationRepository.findByCityAndStreetAndHouseNumberAndActiveIsTrue(
                    location.getCity(), location.getStreet(), location.getHouseNumber()
            ).orElseThrow(() -> {
                logger.error("Active pickup location not found for {}, {}, {}", location.getCity(), location.getStreet(), location.getHouseNumber());
                return new ValidationException("The pick-up location is either not found or inactive");
            });

            orderToUpdate.setPickupLocation(locationToSet);
            orderToUpdate.setAddress(null);
        }

        Order updated = orderRepository.save(orderToUpdate);
        logger.info("Order {} successfully updated", updated.getId());
        return orderMapper.mapEntityToResponse(updated);
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
