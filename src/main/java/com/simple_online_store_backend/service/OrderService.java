package com.simple_online_store_backend.service;

import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.dto.order.OrderResponseDTO;
import com.simple_online_store_backend.entity.*;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.exception.OrderStatusException;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.AddressMapper;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.mapper.PickupLocationMapper;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PeopleRepository peopleRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final PickupLocationRepository pickupLocationRepository;
    private final PickupLocationMapper pickupLocationMapper;
    private final AddressMapper addressMapper;
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Transactional
    @PreAuthorize("hasRole('ROLE_USER')")
    public OrderResponseDTO createOrder(OrderRequestDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        //TODO
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
            Address addressToSet = addressMapper.mapRequestDTOToAddress(dto.getAddress());
            addressRepository.save(addressToSet);
            order.setAddress(addressToSet);
        }

        order.setPerson(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setProducts(products);

        orderRepository.save(order);
        return orderMapper.mapEntityToResponse(order);
    }

    @PreAuthorize("isAuthenticated()")
    public OrderResponseDTO getOrderByOrderId(int orderId) {
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

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public OrderResponseDTO updateOrder(int orderId, OrderRequestDTO dto) {
        Order orderToUpdate = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order with ID " + orderId + " not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        String role = personDetails.getAuthorities().stream().findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        if (!role.equals("ROLE_ADMIN")) {
            if (!orderToUpdate.getPerson().getId().equals(personDetails.getId())) {
                throw new AccessDeniedException("You are not authorized to perform this action");
            }
        }

        if (!orderToUpdate.getStatus().equals(OrderStatus.PENDING)) {
            throw new OrderStatusException("Only orders with PENDING status can be updated");
        }

        // Проверка, что аккаунт не деактивирован (не заблокирован)
        if (!personDetails.isAccountNonLocked()) {
            throw new ValidationException("Your account is deactivated. Please restore your account before placing an order.");
        }

        List<Product> products = dto.getProductsIds().stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new ValidationException("Product with ID " + id + " not found")))
                .toList();

        if (products.stream().anyMatch(p -> !Boolean.TRUE.equals(p.getAvailability()))) {
            throw new ValidationException("Some products are not available for order");
        }

        orderToUpdate.setProducts(products);

        logger.debug("Order saved with new products OK?");
        if (dto.getAddress() != null) {
            Address addressFromDTO = addressMapper.mapRequestDTOToAddress(dto.getAddress());
            Address addressToSet = getOrCreateAddress(addressFromDTO);
            orderToUpdate.setAddress(addressToSet);
            orderToUpdate.setPickupLocation(null);
        } else if (dto.getPickupLocation() != null) {
            PickupLocation locationFromDTO = pickupLocationMapper.mapRequestTOPickupLocation(dto.getPickupLocation());
            Optional<PickupLocation> optional = pickupLocationRepository.findByCityAndStreetAndHouseNumber(
                    locationFromDTO.getCity(),
                    locationFromDTO.getStreet(),
                    locationFromDTO.getHouseNumber()
            );

            if (optional.isEmpty() || !optional.get().getActive()) {
                throw new ValidationException("Этот пункт выдачи не существует или не активен");
            }

            orderToUpdate.setPickupLocation(optional.get());
            orderToUpdate.setAddress(null);
        }

        // ❗️Не теряем статус и другие поля
        // orderToUpdate.setStatus(OrderStatus.PENDING); // не нужно менять, если он уже PENDING

        logger.debug("orderToUpdate.id before save: " + orderToUpdate.getId());
        logger.debug("orderToUpdate.address.id before save: "
                + (orderToUpdate.getAddress() != null ? orderToUpdate.getAddress().getId() : "null"));
        logger.debug("orderToUpdate.person.id before save: "
                + (orderToUpdate.getPerson() != null ? orderToUpdate.getPerson().getId() : "null"));
        orderRepository.save(orderToUpdate);
        return orderMapper.mapEntityToResponse(orderToUpdate);
    }

    /*private PickupLocation getOrCreatePickupLocation(PickupLocation locationFromDTO) {
        Optional<PickupLocation> exitingLocation = pickupLocationRepository.findByCityAndStreetAndHouseNumber(
                locationFromDTO.getCity(), locationFromDTO.getStreet(), locationFromDTO.getHouseNumber());
        return exitingLocation.orElseGet(() -> pickupLocationRepository.save(locationFromDTO));
    }*/

    private Address getOrCreateAddress(Address addressFromDTO) {
        Optional<Address> exiting = addressRepository
                .findByCityAndStreetAndHouseNumberAndApartment(addressFromDTO.getCity(), addressFromDTO.getStreet(),
                        addressFromDTO.getHouseNumber(), addressFromDTO.getApartment());
        if (exiting.isPresent()) {
            return exiting.get();
        } else {
            addressFromDTO.setId(null); // важно! иначе merge может запутаться
            return addressRepository.save(addressFromDTO); // возвращает managed entity
        }
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
