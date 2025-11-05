package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.security.PersonDetails;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PeopleControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PeopleRepository peopleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDB() {
        peopleRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Nested
    class methodGetAllCustomers {

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAllCustomers_asAdmin_returnsOnlyRoleUser_and200() throws Exception {
            // given: в БД есть два обычных пользователя и один админ
            savePerson("alice", "alice@test.io", "ROLE_USER", LocalDate.of(1990, 1, 1), "+49-111", false, "Test234");
            savePerson("bob", "bob@test.io", "ROLE_USER", LocalDate.of(1992, 2, 2), "+49-222", false, "Test234");
            savePerson("charlie", "charlie@test.io", "ROLE_ADMIN", LocalDate.of(1985, 3, 3), "+49-333", false, "Test234");

            // when/then
            mockMvc.perform(get("/people/all-customers").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // возвращается только два клиента с ROLE_USER
                    .andExpect(jsonPath("$", hasSize(2)))
                    // проверка структуры DTO (важные поля присутствуют)
                    .andExpect(jsonPath("$[*].id", everyItem(notNullValue())))
                    .andExpect(jsonPath("$[*].userName", containsInAnyOrder("alice", "bob")))
                    .andExpect(jsonPath("$[*].email", containsInAnyOrder("alice@test.io", "bob@test.io")))
                    .andExpect(jsonPath("$[*].role", everyItem(is("ROLE_USER"))))
                    // доп. поля DTO, если они маппятся (dateOfBirth/phoneNumber); если в маппере их нет, эти проверки можно убрать
                    .andExpect(jsonPath("$[?(@.userName=='alice')].dateOfBirth", notNullValue()))
                    .andExpect(jsonPath("$[?(@.userName=='alice')].phoneNumber", notNullValue()))
                    // убеждаемся, что чувствительных полей (например, password) нет
                    .andExpect(jsonPath("$[*].password").doesNotExist());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAllCustomers_asAdmin_whenNoCustomers_returnsEmptyList_and200() throws Exception {
            savePerson("admin", "admin@test.io", "ROLE_ADMIN", LocalDate.of(1980, 1, 1), "+49-000", false, "Test234");

            mockMvc.perform(get("/people/all-customers").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser(roles = "USER")
        void getAllCustomers_asUser_isForbidden403() throws Exception {
            mockMvc.perform(get("/people/all-customers"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getAllCustomers_unauthenticated_isUnauthorized401() throws Exception {
            mockMvc.perform(get("/people/all-customers"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class methodDeactivateAccount {
        @Test
        @DisplayName("deactivateAccount: 200 OK, user marked deleted, PENDING/PROCESSING orders -> CANCELLED")
        void deactivateAccount_success_marksUserDeleted_andCancelsOpenOrders() throws Exception {
            // given: пользователь + 3 заказа (PENDING, PROCESSING, CANCELLED)
            Person user = savePerson("alice", "alice@test.io", "ROLE_USER", LocalDate.of(1990,1,1), "+49-111", false, "pwd");
            saveOrder(user, OrderStatus.PENDING);
            saveOrder(user, OrderStatus.PROCESSING);
            saveOrder(user, OrderStatus.CANCELLED);

            // аутентифицируемся как этот пользователь (контроллер кастует к PersonDetails) :contentReference[oaicite:1]{index=1}
            UserDetails principal = new PersonDetails(user);

            // when/then
            mockMvc.perform(patch("/people/deactivate-account")
                            .with(user(principal)) // кладём PersonDetails в SecurityContext
                            .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Account has been deactivated."))); // контроллер возвращает String :contentReference[oaicite:2]{index=2}

            // verify: пользователь помечен удалённым
            Person reloaded = peopleRepository.findById(user.getId()).orElseThrow();
            assertTrue(reloaded.getDeleted(), "User must be marked as deleted");

            // verify: открытые заказы отменены, прочие не тронуты (сервисная логика) :contentReference[oaicite:3]{index=3}
            var orders = orderRepository.findByPerson(reloaded);
            long cancelled = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
            long processing = orders.stream().filter(o -> o.getStatus() == OrderStatus.PROCESSING).count();
            long pending = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();

            assertEquals(3, orders.size(), "We created exactly 3 orders for the user");
            assertEquals(3, cancelled, "All PENDING/PROCESSING must be CANCELLED; existing CANCELLED remains CANCELLED");
            assertEquals(0, processing, "No PROCESSING orders should remain");
            assertEquals(0, pending, "No PENDING orders should remain");
        }

        @Test
        @DisplayName("deactivateAccount: 401 UNAUTHORIZED for unauthenticated request")
        void deactivateAccount_unauthenticated_returns401() throws Exception {
            // no auth at all → JWTFilter отдаст 401 (эндпоинт не в whitelist)
            mockMvc.perform(patch("/people/deactivate-account"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private Order saveOrder(Person person, OrderStatus status) {
        Order o = new Order();
        o.setPerson(person);
        o.setStatus(status);
        return orderRepository.save(o); // реальный репозиторий заказов :contentReference[oaicite:6]{index=6}
    }

    private Person savePerson(String userName,
                              String email,
                              String role,
                              LocalDate dob,
                              String phone,
                              boolean deleted,
                              String rawPassword) {
        Person p = new Person();
        p.setUserName(userName);
        p.setEmail(email);
        p.setRole(role);
        p.setDateOfBirth(dob);
        p.setPhoneNumber(phone);
        p.setDeleted(deleted);
        // В сущности пароль NOT NULL — сохраняем простой тестовый пароль
        p.setPassword(rawPassword == null ? "test-password" : rawPassword);
        return peopleRepository.save(p);
    }
}
