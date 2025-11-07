package com.simple_online_store_backend.controller.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "demo.helpers.enabled=true"
})
class AccountDemoControllerTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PeopleRepository peopleRepository;

    @MockitoSpyBean PeopleService peopleService;

    @BeforeEach
    void setup() {
        peopleRepository.deleteAll();
        Mockito.reset(peopleService);
        SecurityContextHolder.clearContext();
    }

    private Person saveUser(String username, String email, String role, boolean locked) {
        Person p = new Person();
        p.setUserName(username);
        p.setEmail(email);
        p.setPassword("encoded");
        p.setRole(role);
        p.setDeleted(locked); // в проекте locked хранится в deleted
        return peopleRepository.save(p);
    }

    private UsernamePasswordAuthenticationToken auth(Person p) {
        var pd = new PersonDetails(p);
        return new UsernamePasswordAuthenticationToken(pd, null,
                java.util.List.of(new SimpleGrantedAuthority(p.getRole())));
    }

    @Nested
    class methodLockTest {

        @Test
        void lock_user_returns200_andPersists() throws Exception {
            var user = saveUser("user", "user@example.com", "ROLE_USER", false);

            mvc.perform(post("/auth/dev/_lock").with(authentication(auth(user)))
                            .param("username", "user")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.username").value("user"))
                    .andExpect(jsonPath("$.locked").value(true));

            var reloaded = peopleRepository.findByUserName("user").orElseThrow();
            Assertions.assertTrue(reloaded.getDeleted(), "user should be locked (deleted=true)");
        }

        @Test
        void unlock_user_returns200_andPersists() throws Exception {
            var user = saveUser("user", "user@example.com", "ROLE_USER", true);

            mvc.perform(post("/auth/dev/_unlock").with(authentication(auth(user)))
                            .param("username", "user")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("user"))
                    .andExpect(jsonPath("$.locked").value(false));

            var reloaded = peopleRepository.findByUserName("user").orElseThrow();
            Assertions.assertFalse(reloaded.getDeleted(), "user should be unlocked (deleted=false)");
        }

        @Test
        void lock_unknownUser_returns404() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN", false);

            mvc.perform(post("/auth/dev/_lock").with(authentication(auth(admin)))
                            .param("username", "ghost")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"))
                    .andExpect(jsonPath("$.path").value("/auth/dev/_lock"));
        }

        @Test
        void lock_serviceThrows_returns500() throws Exception {
            var admin = saveUser("root", "root@example.com", "ROLE_ADMIN", false);
            var token = auth(admin);
            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                doThrow(new RuntimeException("DB down")).when(peopleService).setLocked(anyString(), anyBoolean());

                mvc.perform(post("/auth/dev/_lock").with(authentication(token))
                                .param("username", "user")
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/auth/dev/_lock"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Nested
    class methodUnlockTest {
        @Test
        void endpoints_notAvailable_whenFeatureFlagDisabled() throws Exception {
            mvc.perform(post("/auth/dev/_lock").param("username", "any").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            mvc.perform(post("/auth/dev/_unlock").param("username", "any").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }
}

