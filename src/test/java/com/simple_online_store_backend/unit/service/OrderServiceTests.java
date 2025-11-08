package com.simple_online_store_backend.unit.service;

// package com.simple_online_store_backend.unit.service;

import com.simple_online_store_backend.dto.order.*;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.PickupLocation;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.exception.ValidationException;
import com.simple_online_store_backend.mapper.OrderMapper;
import com.simple_online_store_backend.repository.*;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock OrderRepository orderRepository;
    @Mock OrderMapper orderMapper;
    @Mock PeopleRepository peopleRepository;
    @Mock ProductRepository productRepository;
    @Mock AddressRepository addressRepository;
    @Mock PickupLocationRepository pickupLocationRepository;

    @InjectMocks OrderService orderService;

    private Person owner;
    private PersonDetails ownerDetails;

    @BeforeEach
    void setupSec() {
        owner = new Person();
        owner.setId(1);
        owner.setUserName("user");
        owner.setDeleted(false);

        ownerDetails = new PersonDetails(owner);

        // Реальный токен с ролью USER
        var auth = new UsernamePasswordAuthenticationToken(
                ownerDetails,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ---------------- createOrder

    @Test
    void createOrder_success_withAddress() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductIds(List.of(10, 20));
        req.setAddressId(5);

        Product p1 = product(10, "A", true);
        Product p2 = product(20, "B", true);
        Address addr = new Address();

        when(peopleRepository.findByUserName("user")).thenReturn(Optional.of(owner));
        when(productRepository.findById(10)).thenReturn(Optional.of(p1));
        when(productRepository.findById(20)).thenReturn(Optional.of(p2));
        when(addressRepository.findById(5)).thenReturn(Optional.of(addr));

        Order saved = new Order(); saved.setId(7); saved.setStatus(OrderStatus.PENDING); saved.setPerson(owner);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        OrderDetailsResponse dto = new OrderDetailsResponse(); dto.setId(7); dto.setStatus(OrderStatus.PENDING);
        when(orderMapper.toDetails(saved)).thenReturn(dto);

        OrderDetailsResponse result = orderService.createOrder(req);

        assertEquals(7, result.getId());
        verify(orderRepository).save(any(Order.class));
        verify(pickupLocationRepository, never()).findById(anyInt());
    }

    @Test
    void createOrder_success_withPickup_active() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductIds(List.of(1));
        req.setPickupLocationId(11);

        Product p = product(1, "X", true);
        PickupLocation pl = new PickupLocation(); pl.setActive(true);

        when(peopleRepository.findByUserName("user")).thenReturn(Optional.of(owner));
        when(productRepository.findById(1)).thenReturn(Optional.of(p));
        when(pickupLocationRepository.findById(11)).thenReturn(Optional.of(pl));

        Order saved = new Order(); saved.setId(9); saved.setStatus(OrderStatus.PENDING); saved.setPerson(owner);
        when(orderRepository.save(any())).thenReturn(saved);
        when(orderMapper.toDetails(saved)).thenReturn(new OrderDetailsResponse());

        assertDoesNotThrow(() -> orderService.createOrder(req));
        verify(addressRepository, never()).findById(anyInt());
    }

    @Test
    void createOrder_fails_whenOwnerDeleted() {
        owner.setDeleted(true);
        when(peopleRepository.findByUserName("user")).thenReturn(Optional.of(owner));

        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductIds(List.of(1));
        req.setAddressId(5);

        assertThrows(ValidationException.class, () -> orderService.createOrder(req));
    }

    @Test
    void createOrder_fails_whenProductNotFound() {
        when(peopleRepository.findByUserName("user")).thenReturn(Optional.of(owner));
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductIds(List.of(1));
        req.setPickupLocationId(2);

        assertThrows(EntityNotFoundException.class, () -> orderService.createOrder(req));
    }

    @Test
    void createOrder_fails_whenAnyProductUnavailable() {
        when(peopleRepository.findByUserName("user")).thenReturn(Optional.of(owner));
        when(productRepository.findById(1)).thenReturn(Optional.of(product(1, "X", false)));

        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductIds(List.of(1));
        req.setPickupLocationId(2);

        assertThrows(ValidationException.class, () -> orderService.createOrder(req));
    }

    @Test
    void createOrder_fails_whenAddressNotFound() {
        when(peopleRepository.findByUserName("user")).thenReturn(Optional.of(owner));
        when(productRepository.findById(1)).thenReturn(Optional.of(product(1, "X", true)));
        when(addressRepository.findById(99)).thenReturn(Optional.empty());

        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductIds(List.of(1));
        req.setAddressId(99);

        assertThrows(EntityNotFoundException.class, () -> orderService.createOrder(req));
    }

    @Test
    void createOrder_fails_whenPickupNotFound_orInactive() {
        when(peopleRepository.findByUserName("user")).thenReturn(Optional.of(owner));
        when(productRepository.findById(1)).thenReturn(Optional.of(product(1, "X", true)));

        OrderCreateRequest req = new OrderCreateRequest();
        req.setProductIds(List.of(1));
        req.setPickupLocationId(5);

        // not found
        when(pickupLocationRepository.findById(5)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.createOrder(req));

        // inactive
        PickupLocation pl = new PickupLocation(); pl.setActive(false);
        when(pickupLocationRepository.findById(5)).thenReturn(Optional.of(pl));
        assertThrows(ValidationException.class, () -> orderService.createOrder(req));
    }

    // ---------------- getOrderById

    @Test
    void getOrderById_allowsAdmin_forAnyOrder() {
        // подменяем токен на ADMIN
        var adminAuth = new UsernamePasswordAuthenticationToken(
                ownerDetails, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        Order order = new Order(); order.setId(3);
        when(orderRepository.findWithDetailsById(3)).thenReturn(Optional.of(order));
        when(orderMapper.toDetails(order)).thenReturn(new OrderDetailsResponse());

        assertDoesNotThrow(() -> orderService.getOrderById(3));
        verify(orderRepository).findWithDetailsById(3);
    }

    @Test
    void getOrderById_allowsOwner_ifUser() {
        Order o = new Order(); o.setId(4);
        Person owner2 = new Person(); owner2.setId(1); // same id as ownerDetails
        o.setPerson(owner2);

        when(orderRepository.findWithDetailsById(4)).thenReturn(Optional.of(o));
        when(orderMapper.toDetails(o)).thenReturn(new OrderDetailsResponse());

        assertDoesNotThrow(() -> orderService.getOrderById(4));
    }

    @Test
    void getOrderById_deniesNonOwner_ifUser() {
        Order o = new Order(); o.setId(4);
        Person other = new Person(); other.setId(999);
        o.setPerson(other);

        when(orderRepository.findWithDetailsById(4)).thenReturn(Optional.of(o));

        assertThrows(AccessDeniedException.class, () -> orderService.getOrderById(4));
    }

    @Test
    void getOrderById_throwsWhenNotFound() {
        when(orderRepository.findWithDetailsById(111)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderById(111));
    }

    // ---------------- findAllOrdersByCustomer

    @Test
    void findAllOrdersByCustomer_mapsListForCurrentUser() {
        Order a = new Order(); a.setId(1);
        Order b = new Order(); b.setId(2);
        when(orderRepository.findByPerson_Id(1)).thenReturn(List.of(a, b));

        OrderListItemResponse r1 = new OrderListItemResponse(); r1.setId(1);
        OrderListItemResponse r2 = new OrderListItemResponse(); r2.setId(2);
        when(orderMapper.toListItem(a)).thenReturn(r1);
        when(orderMapper.toListItem(b)).thenReturn(r2);

        List<OrderListItemResponse> list = orderService.findAllOrdersByCustomer();

        assertEquals(2, list.size());
        assertEquals(1, list.get(0).getId());
        assertEquals(2, list.get(1).getId());
    }

    // ---------------- cancelOrder

    @Test
    void cancelOrder_success_forOwnerAndPending() {
        Order o = new Order(); o.setId(5); o.setStatus(OrderStatus.PENDING); o.setPerson(owner);

        when(orderRepository.findById(5)).thenReturn(Optional.of(o));
        when(orderMapper.mapEntityToResponse(o)).thenReturn(new OrderResponseDTO());

        OrderResponseDTO dto = orderService.cancelOrder(5);

        assertNotNull(dto);
        assertEquals(OrderStatus.CANCELLED, o.getStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void cancelOrder_deniesWhenNotOwner() {
        Order o = new Order(); o.setId(5); o.setStatus(OrderStatus.PENDING);
        Person other = new Person(); other.setId(999); o.setPerson(other);
        when(orderRepository.findById(5)).thenReturn(Optional.of(o));

        assertThrows(AccessDeniedException.class, () -> orderService.cancelOrder(5));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_failsWhenNotPending() {
        Order o = new Order(); o.setId(5); o.setStatus(OrderStatus.PROCESSING); o.setPerson(owner);
        when(orderRepository.findById(5)).thenReturn(Optional.of(o));

        assertThrows(ValidationException.class, () -> orderService.cancelOrder(5));
    }

    @Test
    void cancelOrder_throwsWhenNotFound() {
        when(orderRepository.findById(404)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.cancelOrder(404));
    }

    // ---------------- reactivateOrder

    @Test
    void reactivateOrder_success_forOwnerAndCancelled() {
        Order o = new Order(); o.setId(6); o.setStatus(OrderStatus.CANCELLED); o.setPerson(owner);
        when(orderRepository.findById(6)).thenReturn(Optional.of(o));
        when(orderMapper.mapEntityToResponse(o)).thenReturn(new OrderResponseDTO());

        OrderResponseDTO dto = orderService.reactivateOrder(6);

        assertNotNull(dto);
        assertEquals(OrderStatus.PENDING, o.getStatus());
        verify(orderRepository).save(o);
    }

    @Test
    void reactivateOrder_deniesWhenNotOwner() {
        Order o = new Order(); o.setId(6); o.setStatus(OrderStatus.CANCELLED);
        Person other = new Person(); other.setId(999); o.setPerson(other);
        when(orderRepository.findById(6)).thenReturn(Optional.of(o));

        assertThrows(AccessDeniedException.class, () -> orderService.reactivateOrder(6));
    }

    @Test
    void reactivateOrder_failsWhenNotCancelled() {
        Order o = new Order(); o.setId(6); o.setStatus(OrderStatus.PENDING); o.setPerson(owner);
        when(orderRepository.findById(6)).thenReturn(Optional.of(o));

        assertThrows(ValidationException.class, () -> orderService.reactivateOrder(6));
    }

    @Test
    void reactivateOrder_throwsWhenNotFound() {
        when(orderRepository.findById(777)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.reactivateOrder(777));
    }

    // ---------------- helpers

    private Product product(int id, String name, boolean available) {
        Product p = new Product();
        p.setId(id);
        p.setProductName(name);
        p.setPrice(new BigDecimal("1.00"));
        p.setAvailability(available);
        return p;
    }
}
