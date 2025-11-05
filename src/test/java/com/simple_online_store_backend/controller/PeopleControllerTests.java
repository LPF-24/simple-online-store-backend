package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.entity.Person;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

    @BeforeEach
    void cleanDB() {
        peopleRepository.deleteAll();
    }

    @Nested
    class methodGetAllCustomers {

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAllCustomers_asAdmin_returnsOnlyRoleUser_and200() throws Exception {
            // given: в БД есть два обычных пользователя и один админ
            savePerson("alice", "alice@test.io", "ROLE_USER", LocalDate.of(1990, 1, 1), "+49-111", false);
            savePerson("bob", "bob@test.io", "ROLE_USER", LocalDate.of(1992, 2, 2), "+49-222", false);
            savePerson("charlie", "charlie@test.io", "ROLE_ADMIN", LocalDate.of(1985, 3, 3), "+49-333", false);

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
            // given: только админ в базе
            savePerson("admin", "admin@test.io", "ROLE_ADMIN", LocalDate.of(1980, 1, 1), "+49-000", false);

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

    private Person savePerson(String userName,
                              String email,
                              String role,
                              LocalDate dob,
                              String phone,
                              boolean deleted) {
        Person p = new Person();
        p.setUserName(userName);
        p.setEmail(email);
        p.setRole(role);
        p.setDateOfBirth(dob);
        p.setPhoneNumber(phone);
        p.setDeleted(deleted);
        p.setPassword("test-password");
        return peopleRepository.save(p);
    }
}
