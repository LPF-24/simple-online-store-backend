package com.simple_online_store_backend.controller;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.simple_online_store_backend.enums.OrderStatus;
import com.simple_online_store_backend.repository.OrderRepository;
import com.simple_online_store_backend.entity.Order;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.security.PersonDetails;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JWTUtil jwtUtil;

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

    @Nested
    class methodRestoreAccount {
        @Test
        @DisplayName("restore-account: 200 OK — locked user becomes active")
        void restoreAccount_success_200() throws Exception {
            String username = "alice";
            String rawPassword = "Secret123!";
            Person user = savePerson("alice", "alice@test.io", "ROLE_USER",
                    LocalDate.of(1990,1,1), "+49-111", true, rawPassword);

            mockMvc.perform(patch("/people/restore-account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON) // было TEXT_PLAIN
                            .content("""
            {"username":"%s","password":"%s"}
        """.formatted(username, rawPassword)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) // было TEXT_PLAIN
                    .andExpect(content().string("Account successfully restored")); // или containsString(...)

            var refreshed = peopleRepository.findById(user.getId()).orElseThrow();
            assertFalse(refreshed.getDeleted(), "User must be unlocked (deleted=false) after restore");
        }

        @Test
        @DisplayName("restore-account: 401 UNAUTHORIZED — bad password")
        void restoreAccount_badPassword_401() throws Exception {
            String username = "bob";

            savePerson(username, "bob@test.io", "ROLE_USER",
                    LocalDate.of(1990,1,1), "+49-111", true, "RightPass1");

            mockMvc.perform(patch("/people/restore-account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                        {"username":"%s","password":"WrongPass"}
                    """.formatted(username)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                    .andExpect(jsonPath("$.path").value("/people/restore-account"));
        }

        @Test
        @DisplayName("restore-account: 500 INTERNAL_ERROR — account already active")
        void restoreAccount_alreadyActive_500() throws Exception {
            // given: пользователь уже активен (deleted=false)
            String username = "charlie";
            savePerson(username, "charlie@test.io", "ROLE_USER", LocalDate.of(1985, 3, 3), "+49-333", false, "Test234");


            mockMvc.perform(post("/people/restore-account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                            {"username":"%s","password":"AnyPass9"}
                        """.formatted(username)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.path").value("/people/restore-account"));
        }
    }

    @Nested
    class methodGetProfile {

        @Test
        @DisplayName("profile: 200 OK — returns the current authenticated user's profile")
        void getProfile_ok_returnsCurrentUser() throws Exception {
            Person alice = savePerson("alice", "alice@test.io", "ROLE_USER",
                    LocalDate.of(1990,1,1), "+49-111", false, "Secret123!");
            // второй пользователь просто для контекста
            savePerson("bob", "bob@test.io", "ROLE_USER",
                    LocalDate.of(1992,2,2), "+49-222", false, "Secret123!");

            UserDetails principal = new PersonDetails(alice);

            mockMvc.perform(get("/people/profile")
                            .with(user(principal))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(alice.getId()))
                    .andExpect(jsonPath("$.userName").value("alice"))
                    .andExpect(jsonPath("$.email").value("alice@test.io"))
                    .andExpect(jsonPath("$.role").value("ROLE_USER"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("profile: 401 UNAUTHORIZED — unauthenticated request")
        void getProfile_unauthenticated_401() throws Exception {
            mockMvc.perform(get("/people/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("profile: 423 ACCOUNT_LOCKED — deactivated user with valid access token")
        void getProfile_locked_423() throws Exception {
            // given: в БД есть заблокированный пользователь
            String username = "alice";
            savePerson(username, "alice@test.io", "ROLE_USER",
                    LocalDate.of(1990,1,1), "+49-111", /*deleted=*/ true, "Secret123!");

            // и валидный access token, который фильтр примет как принадлежащий alice
            String accessToken = "ACCESS.ALICE";
            DecodedJWT decoded = mock(DecodedJWT.class);
            Claim usernameClaim = mock(Claim.class);
            when(usernameClaim.asString()).thenReturn(username);
            when(decoded.getClaim("username")).thenReturn(usernameClaim);
            when(jwtUtil.validateToken(accessToken)).thenReturn(decoded);

            // when/then: запрос идёт через JWTFilter → аккаунт помечен deleted=true → 423
            mockMvc.perform(get("/people/profile")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isLocked()) // 423
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(423))
                    .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/people/profile"));
        }
    }

    @Nested
    class methodPromoteTests {
        // ВАРИАНТ 1 (проще): если можно – добавьте на ВЕСЬ класс PeopleControllerTests:
// @TestPropertySource(properties = "admin.registration.code=MASTER-CODE")

// ВАРИАНТ 2: если нельзя менять аннотации класса – добавьте внутрь PeopleControllerTests:
/*
@DynamicPropertySource
static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("admin.registration.code", () -> "MASTER-CODE");
}
*/

        @Nested
        class methodPromote {

            @Test
            @DisplayName("promote: 200 OK — ROLE_USER повышается до ROLE_ADMIN, тело содержит message")
            void promote_success_200_updatesRole_andReturnsMessage() throws Exception {
                Person alice = savePerson("alice", "alice@test.io", "ROLE_USER",
                        LocalDate.of(1990,1,1), "+49-111", false, "Secret123!");

                UserDetails principal = new PersonDetails(alice);

                mockMvc.perform(patch("/people/promote")
                                .with(user(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                            {"code":"work2025admin"}
                        """))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message")
                                .value("You have been successfully promoted to administrator. Please log in again, colleague."));

                // verify: роль обновлена в БД
                Person refreshed = peopleRepository.findById(alice.getId()).orElseThrow();
                org.assertj.core.api.Assertions.assertThat(refreshed.getRole()).isEqualTo("ROLE_ADMIN");
            }

            @Test
            @DisplayName("promote: 401 UNAUTHORIZED — запрос без аутентификации")
            void promote_unauthenticated_401() throws Exception {
                mockMvc.perform(patch("/people/promote")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"code":"MASTER-CODE"}
                                        """))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("promote: 400 BAD REQUEST — валидация кода не пройдена (пустая строка)")
            void promote_validationError_400_whenCodeInvalid() throws Exception {
                // given
                Person bob = savePerson("bob", "bob@test.io", "ROLE_USER",
                        LocalDate.of(1992,2,2), "+49-222", false, "Secret123!");
                UserDetails principal = new PersonDetails(bob);

                mockMvc.perform(patch("/people/promote")
                                .with(user(principal))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"code":""}
                                        """))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                        .andExpect(jsonPath("$.path").value("/people/promote"));
            }
        }
    }

    private Order saveOrder(Person person, OrderStatus status) {
        Order o = new Order();
        o.setPerson(person);
        o.setStatus(status);
        return orderRepository.save(o); // реальный репозиторий заказов :contentReference[oaicite:6]{index=6}
    }

    private Person savePerson(String userName, String email, String role,
                              LocalDate dob, String phone, boolean deleted,
                              String rawPassword) {
        Person p = new Person();
        p.setUserName(userName);
        p.setEmail(email);
        p.setRole(role);
        p.setDateOfBirth(dob);
        p.setPhoneNumber(phone);
        p.setDeleted(deleted);
        p.setPassword(passwordEncoder.encode(rawPassword)); // ← важно
        return peopleRepository.save(p);
    }
}
