package com.simple_online_store_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple_online_store_backend.dto.order.OrderRequestDTO;
import com.simple_online_store_backend.entity.Address;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.entity.Product;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.enums.ProductCategory;
import com.simple_online_store_backend.repository.AddressRepository;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.repository.ProductRepository;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.OrderService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired PeopleRepository peopleRepository;
    @MockitoSpyBean OrderRepository orderRepository;
    @Autowired ProductRepository productRepository;
    @Autowired AddressRepository addressRepository;

    @MockitoSpyBean
    OrderService orderService;

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
        peopleRepository.deleteAll();
        Mockito.reset(orderService);
    }

    private Person saveUser(String username, String email, String role) {
        Person p = new Person();
        p.setUserName(username);
        p.setEmail(email);
        p.setPassword("encoded");
        p.setRole(role);
        p.setDeleted(false);
        return peopleRepository.save(p);
    }

    private UsernamePasswordAuthenticationToken auth(Person p) {
        var pd = new PersonDetails(p); // твой реальный principal с id/username/role
        var ga = List.of(new SimpleGrantedAuthority(p.getRole()));
        return new UsernamePasswordAuthenticationToken(pd, null, ga);
    }

    private Order order(Person owner, OrderStatus status) {
        Order o = new Order();
        o.setPerson(owner);
        o.setStatus(status);
        // при необходимости добавь address/pickup/products — маппер их поддерживает в DTO, но не обязательны для 200
        return orderRepository.save(o);
    }

    @Nested
    class methodAllOrdersByCustomer {
        // ============ 1) 200 OK: пользователь видит свой список заказов ============
        @Test
        void allMyOrders_success_returnsList() throws Exception {
            Person user = saveUser("maria", "maria@example.com", "ROLE_USER");
            Person other = saveUser("john", "john@example.com", "ROLE_USER");

            // заказы пользователя
            var o1 = order(user, OrderStatus.PENDING);
            var o2 = order(user, OrderStatus.CANCELLED);
            // чужой заказ
            order(other, OrderStatus.PENDING);

            mvc.perform(get("/orders/all-my-orders")
                            .with(authentication(auth(user)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    // ожидаем только 2 заказа текущего пользователя
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", hasItems(o1.getId(), o2.getId())))
                    .andExpect(jsonPath("$[*].status", hasItems("PENDING", "CANCELLED")));
        }

        // ============ 2) 200 OK: у пользователя пока нет заказов — пустой список ============
        @Test
        void allMyOrders_empty_returnsEmptyList() throws Exception {
            Person user = saveUser("empty", "empty@example.com", "ROLE_USER");

            mvc.perform(get("/orders/all-my-orders")
                            .with(authentication(auth(user)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        // ============ 3) 401 UNAUTHORIZED: нет аутентификации (без токена/принципала) ============
        @Test
        void allMyOrders_unauthorized_returns401() throws Exception {
            mvc.perform(get("/orders/all-my-orders").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED")
                    )))
                    .andExpect(jsonPath("$.path").value("/orders/all-my-orders"));
        }

        // ============ 4) 403 FORBIDDEN: роль ADMIN (метод в сервисе требует hasRole('ROLE_USER')) ============
        @Test
        void allMyOrders_forbidden_admin_returns403() throws Exception {
            Person admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            mvc.perform(get("/orders/all-my-orders")
                            .with(authentication(auth(admin)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))))
                    .andExpect(jsonPath("$.path").value("/orders/all-my-orders"));
        }

        // ============ 5) 500 INTERNAL_SERVER_ERROR: сервис упал ============
        @Test
        void allMyOrders_serviceThrows_returns500() throws Exception {
            Person user = saveUser("kate", "kate@example.com", "ROLE_USER");
            var authToken = auth(user);

            SecurityContextHolder.getContext().setAuthentication(authToken);
            try {
                doThrow(new RuntimeException("DB down"))
                        .when(orderService).findAllOrdersByCustomer();

                mvc.perform(get("/orders/all-my-orders")
                                .with(authentication(authToken))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/orders/all-my-orders"));

            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Nested
    class methodGetOrderTests {
        // ===== 200: владелец читает свой заказ =====
        @Test
        void getOrder_ownerUser_returns200() throws Exception {
            Person owner = saveUser("maria", "maria@example.com", "ROLE_USER");
            Order ord = order(owner, OrderStatus.PENDING);

            mvc.perform(get("/orders/{id}", ord.getId())
                            .with(authentication(auth(owner)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ord.getId()))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        // ===== 200: админ может читать любой заказ =====
        @Test
        void getOrder_adminCanViewAnyOrder_returns200() throws Exception {
            Person owner = saveUser("john", "john@example.com", "ROLE_USER");
            Person admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            Order ord = order(owner, OrderStatus.CANCELLED);

            mvc.perform(get("/orders/{id}", ord.getId())
                            .with(authentication(auth(admin)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ord.getId()))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        // ===== 403: чужой заказ обычному пользователю запрещён =====
        @Test
        void getOrder_otherUser_returns403() throws Exception {
            Person owner = saveUser("kate", "kate@example.com", "ROLE_USER");
            Person other = saveUser("nick", "nick@example.com", "ROLE_USER");
            Order ord = order(owner, OrderStatus.PENDING);

            mvc.perform(get("/orders/{id}", ord.getId())
                            .with(authentication(auth(other)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))))
                    .andExpect(jsonPath("$.path").value("/orders/" + ord.getId()));
        }

        // ===== 404: заказ не найден =====
        @Test
        void getOrder_notFound_returns404() throws Exception {
            Person user = saveUser("ghost", "ghost@example.com", "ROLE_USER");

            mvc.perform(get("/orders/{id}", 999999)
                            .with(authentication(auth(user)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ENTITY_NOT_FOUND"), equalTo("NOT_FOUND"))))
                    .andExpect(jsonPath("$.message", containsString("not found")))
                    .andExpect(jsonPath("$.path").value("/orders/999999"));
        }

        // ===== 401: нет аутентификации (без токена/принципала) =====
        @Test
        void getOrder_unauthorized_returns401() throws Exception {
            // без .with(authentication(...))
            mvc.perform(get("/orders/{id}", 1).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED")
                    )));
        }

        // ===== 500: сервис упал при обработке =====
        @Test
        void getOrder_serviceThrows_returns500() throws Exception {
            Person user = saveUser("peter", "peter@example.com", "ROLE_USER");
            Order ord = order(user, OrderStatus.PENDING);

            var authToken = auth(user);
            // из-за @PreAuthorize на сервисе стабы через spy требуют аутентификацию в контексте
            SecurityContextHolder.getContext().setAuthentication(authToken);
            try {
                doThrow(new RuntimeException("DB down"))
                        .when(orderService).getOrderById(ord.getId());

                mvc.perform(get("/orders/{id}", ord.getId())
                                .with(authentication(authToken))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/orders/" + ord.getId()));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Nested
    class methodAddOrderTests {

        @BeforeEach
        void clean() {
            orderRepository.deleteAll();
            addressRepository.deleteAll();
            productRepository.deleteAll();
            peopleRepository.deleteAll();
            Mockito.reset(orderService, orderRepository);
            SecurityContextHolder.clearContext();
        }

        private Person saveUser(String username, String email, String role, boolean deleted) {
            Person p = new Person();
            p.setUserName(username);
            p.setEmail(email);
            p.setPassword("encoded");
            p.setRole(role);
            p.setDeleted(deleted);
            return peopleRepository.save(p);
        }

        private UsernamePasswordAuthenticationToken auth(Person p) {
            var pd = new PersonDetails(p);
            return new UsernamePasswordAuthenticationToken(
                    pd, null, List.of(new SimpleGrantedAuthority(p.getRole())));
        }

        private Product saveProduct(String name, boolean available, BigDecimal price) {
            Product pr = new Product();
            pr.setProductName(name);
            pr.setProductDescription(name + " desc");
            pr.setProductCategory(ProductCategory.COMPONENTS);
            pr.setPrice(price);
            pr.setAvailability(available);
            return productRepository.save(pr);
        }

        private Address saveAddress(String city, String street, String house, String apt, String zip) {
            Address a = new Address();
            a.setCity(city);
            a.setStreet(street);
            a.setHouseNumber(house);
            a.setApartment(apt);
            a.setPostalCode(zip);
            return addressRepository.save(a);
        }

        private String json(Object dto) throws Exception {
            return objectMapper.writeValueAsString(dto);
        }

        private com.simple_online_store_backend.dto.order.OrderCreateRequest req(
                List<Integer> productIds, Integer addressId, Integer pickupLocationId) {
            var r = new com.simple_online_store_backend.dto.order.OrderCreateRequest();
            r.setProductIds(productIds);
            r.setAddressId(addressId);
            r.setPickupLocationId(pickupLocationId);
            return r;
        }

        // ===== 1) 200 OK: успешное создание (доставка по адресу) =====
        @Test
        void createOrder_success_withAddress_returns200() throws Exception {
            Person user = saveUser("maria", "maria@example.com", "ROLE_USER", false);
            Product p1 = saveProduct("Phone", true, new BigDecimal("499.99"));
            Product p2 = saveProduct("Case",  true, new BigDecimal("19.99"));
            Address addr = saveAddress("Berlin", "Main Street", "12A", "45", "10115");

            var dto = req(List.of(p1.getId(), p2.getId()), addr.getId(), null);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.items", hasSize(2)))
                    .andExpect(jsonPath("$.address.city").value("Berlin"))
                    .andExpect(jsonPath("$.pickup").doesNotExist());

            assertThat(orderRepository.count(), greaterThan(0L));
        }

        // ===== 2) 200 OK: самовывоз (pickup), адрес не указан =====
        @Test
        void createOrder_success_withPickup_returns200() throws Exception {
            Person user = saveUser("nick", "nick@example.com", "ROLE_USER", false);
            Product p1 = saveProduct("Mouse", true, new BigDecimal("25.00"));

            // тут можно использовать существующий pickup из сидера/БД; если его нет — пропусти тест или создай перед этим
            // Для простоты используем addressId=null и pickupLocationId=1, если сидер создаёт его с id=1.
            var dto = req(List.of(p1.getId()), null, 1);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.address").doesNotExist())
                    .andExpect(jsonPath("$.pickup").exists());
        }

        // ===== 3) 400: пустой список productIds =====
        @Test
        void createOrder_validationError_emptyProducts_returns400() throws Exception {
            Person user = saveUser("kate", "kate@example.com", "ROLE_USER", false);
            Address addr = saveAddress("Berlin", "Main", "1", "1", "10115");

            var dto = req(List.of(), addr.getId(), null);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", anyOf(equalTo("VALIDATION_ERROR"), equalTo("BAD_REQUEST"))))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }

        // ===== 4) 404: товар не существует (EntityNotFoundException) =====
        @Test
        void createOrder_productNotFound_returns400() throws Exception {
            Person user = saveUser("john", "john@example.com", "ROLE_USER", false);
            Address addr = saveAddress("Berlin", "Main Street", "12A", "45", "10115");

            var dto = req(List.of(999999), addr.getId(), null);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("Product with ID")))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }

        // ===== 5) 400: товар недоступен (availability=false) =====
        @Test
        void createOrder_productUnavailable_returns400() throws Exception {
            Person user = saveUser("peter", "peter@example.com", "ROLE_USER", false);
            Product unavailable = saveProduct("Adapter", false, new BigDecimal("9.99"));
            Address addr = saveAddress("Berlin", "Main", "1", "1", "10115");

            var dto = req(List.of(unavailable.getId()), addr.getId(), null);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("Some products are not available")))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }

        // ===== 6) 400: аккаунт пользователя помечен как deleted =====
        @Test
        void createOrder_userDeleted_returns400() throws Exception {
            Person user = saveUser("blocked", "blocked@example.com", "ROLE_USER", true);
            Product p1 = saveProduct("SSD", true, new BigDecimal("99.00"));
            Address addr = saveAddress("Berlin", "Main", "1", "1", "10115");

            var dto = req(List.of(p1.getId()), addr.getId(), null);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message", containsString("deactivated")))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }

        // ===== 7) 400: некорректный JSON (MESSAGE_NOT_READABLE) =====
        @Test
        void createOrder_malformedJson_returns400() throws Exception {
            Person user = saveUser("json", "json@example.com", "ROLE_USER", false);
            String malformed = "{ \"productIds\": [1, 2], \"addressId\": 1 "; // обрываем JSON

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformed))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", anyOf(equalTo("MESSAGE_NOT_READABLE"), equalTo("BAD_REQUEST"))))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }

        // ===== 8) 401: нет аутентификации =====
        @Test
        void createOrder_unauthorized_returns401() throws Exception {
            // подготовим валидные ids, чтобы валидация не сработала раньше EntryPoint-а
            Product p1 = saveProduct("Keyboard", true, new BigDecimal("49.99"));
            Address addr = saveAddress("Berlin", "Main", "1", "1", "10115");
            var dto = req(List.of(p1.getId()), addr.getId(), null);

            mvc.perform(post("/orders/create-order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED"))));
        }

        // ===== 9) 403: роль ADMIN (эндпоинт только для ROLE_USER) =====
        @Test
        void createOrder_forbidden_admin_returns403() throws Exception {
            Person admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN", false);
            Product p1 = saveProduct("Monitor", true, new BigDecimal("199.00"));
            Address addr = saveAddress("Berlin", "Main", "1", "1", "10115");

            var dto = req(List.of(p1.getId()), addr.getId(), null);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(admin)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }

        // ===== 10) 404: адрес не найден =====
        @Test
        void createOrder_addressNotFound_returns404() throws Exception {
            Person user = saveUser("anna", "anna@example.com", "ROLE_USER", false);
            Product p1 = saveProduct("Cable", true, new BigDecimal("5.99"));

            var dto = req(List.of(p1.getId()), 999_999, null);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ENTITY_NOT_FOUND"), equalTo("NOT_FOUND"))))
                    .andExpect(jsonPath("$.message", containsString("Address not found")))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }

        // ===== 11) 404: пункт выдачи не найден =====
        @Test
        void createOrder_pickupNotFound_returns404() throws Exception {
            Person user = saveUser("olga", "olga@example.com", "ROLE_USER", false);
            Product p1 = saveProduct("Card", true, new BigDecimal("10.00"));

            var dto = req(List.of(p1.getId()), null, 999_999);

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ENTITY_NOT_FOUND"), equalTo("NOT_FOUND"))))
                    .andExpect(jsonPath("$.message", containsString("Pickup location not found")))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }

        // ===== 12) 500: падение на сохранении заказа =====
        @Test
        void createOrder_serviceThrows_returns500() throws Exception {
            Person user = saveUser("fail", "fail@example.com", "ROLE_USER", false);
            Product p1 = saveProduct("Drive", true, new BigDecimal("59.99"));
            Address addr = saveAddress("Berlin", "Main", "1", "1", "10115");

            var dto = req(List.of(p1.getId()), addr.getId(), null);

            // Роняем репозиторий при сохранении (после прохождения всех проверок сервиса)
            doThrow(new RuntimeException("DB down"))
                    .when(orderRepository)
                    .save(org.mockito.ArgumentMatchers.any(com.simple_online_store_backend.entity.Order.class));

            mvc.perform(post("/orders/create-order")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(dto)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                    .andExpect(jsonPath("$.path").value("/orders/create-order"));
        }
    }

    @Nested
    class methodAdminGetAllOrders {

        private Order orderWithProducts(Person owner, OrderStatus status, int productCount) {
            Order o = new Order();
            o.setPerson(owner);
            o.setStatus(status);

            var products = new java.util.ArrayList<Product>();
            for (int i = 0; i < productCount; i++) {
                Product pr = new Product();
                pr.setProductName("AdminList-" + i + "-" + System.nanoTime());
                pr.setProductDescription("Some description");
                pr.setProductCategory(ProductCategory.COMPONENTS); // <-- ВАЖНО: NOT NULL
                pr.setPrice(new java.math.BigDecimal("10.00"));
                pr.setAvailability(Boolean.TRUE);
                products.add(productRepository.save(pr));
            }
            o.setProducts(new java.util.ArrayList<>(products));
            return orderRepository.save(o);
        }

        @BeforeEach
        void clean() {
            orderRepository.deleteAll();
            productRepository.deleteAll();
            peopleRepository.deleteAll();
        }

        // ===== 200 OK: админ видит список укороченных элементов =====
        @Test
        void getAllOrders_admin_success_returnsListItems() throws Exception {
            Person admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");
            Person u1 = saveUser("maria", "maria@example.com", "ROLE_USER");
            Person u2 = saveUser("john", "john@example.com", "ROLE_USER");

            Order o1 = orderWithProducts(u1, OrderStatus.PENDING,   2);
            Order o2 = orderWithProducts(u2, OrderStatus.SHIPPED,   1);
            Order o3 = orderWithProducts(u2, OrderStatus.CANCELLED, 3);

            mvc.perform(get("/orders")
                            .with(authentication(auth(admin)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].status").exists())
                    .andExpect(jsonPath("$[0].productCount").exists())
                    .andExpect(jsonPath("$[?(@.id==" + o1.getId() + ")].productCount").value(hasItem(2)))
                    .andExpect(jsonPath("$[?(@.id==" + o1.getId() + ")].status").value(hasItem("PENDING")))
                    .andExpect(jsonPath("$[?(@.id==" + o2.getId() + ")].productCount").value(hasItem(1)))
                    .andExpect(jsonPath("$[?(@.id==" + o2.getId() + ")].status").value(hasItem("SHIPPED")))
                    .andExpect(jsonPath("$[?(@.id==" + o3.getId() + ")].productCount").value(hasItem(3)))
                    .andExpect(jsonPath("$[?(@.id==" + o3.getId() + ")].status").value(hasItem("CANCELLED")));
        }

        // ===== 200 OK: пустая БД → []
        @Test
        void getAllOrders_admin_empty_returnsEmptyArray() throws Exception {
            Person admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            mvc.perform(get("/orders")
                            .with(authentication(auth(admin)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }

        // ===== 403: доступ обычному пользователю запрещён
        @Test
        void getAllOrders_user_forbidden_returns403() throws Exception {
            Person user = saveUser("user", "user@example.com", "ROLE_USER");
            // создадим один заказ для надёжности
            orderWithProducts(user, OrderStatus.PENDING, 1);

            mvc.perform(get("/orders")
                            .with(authentication(auth(user)))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("ACCESS_DENIED"), equalTo("FORBIDDEN"))))
                    .andExpect(jsonPath("$.path").value("/orders"));
        }

        // ===== 401: без аутентификации
        @Test
        void getAllOrders_unauthorized_returns401() throws Exception {
            mvc.perform(get("/orders").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code", anyOf(
                            equalTo("UNAUTHORIZED"),
                            equalTo("INVALID_ACCESS_TOKEN"),
                            equalTo("TOKEN_EXPIRED")
                    )))
                    .andExpect(jsonPath("$.path").value("/orders"));
        }
    }
}

