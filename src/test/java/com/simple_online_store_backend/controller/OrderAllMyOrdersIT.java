package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.repository.PeopleRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderAllMyOrdersIT {

    @Autowired MockMvc mvc;

    @Autowired PeopleRepository peopleRepository;
    @Autowired OrderRepository orderRepository;

    @MockitoSpyBean
    OrderService orderService; // только для сценария 500

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
}

