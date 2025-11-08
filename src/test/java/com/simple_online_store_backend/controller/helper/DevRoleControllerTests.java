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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "demo.helpers.enabled=true"
})
class DevRoleControllerTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired PeopleRepository peopleRepository;

    @MockitoSpyBean PeopleService peopleService;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void stubRedis() {
        RedisConnection mockConn = mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(mockConn);

        RedisKeyCommands keyCmds = mock(RedisKeyCommands.class);
        when(mockConn.keyCommands()).thenReturn(keyCmds);

        peopleRepository.deleteAll();
        Mockito.reset(peopleService);
        SecurityContextHolder.clearContext();
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
        var pd = new PersonDetails(p);
        return new UsernamePasswordAuthenticationToken(pd, null,
                java.util.List.of(new SimpleGrantedAuthority(p.getRole())));
    }

    @Nested
    class methodDemoteDev {
        @Test
        void demote_admin_returns200_andPersists() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            mvc.perform(patch("/auth/dev/_demote")
                            .with(authentication(auth(admin)))
                            .param("username", "admin")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", containsString("demoted")));

            var reloaded = peopleRepository.findByUserName("admin").orElseThrow();
            Assertions.assertEquals("ROLE_USER", reloaded.getRole());
        }

        @Test
        void demote_alreadyUser_returns200_idempotent() throws Exception {
            var user = saveUser("maria", "maria@example.com", "ROLE_USER");

            mvc.perform(patch("/auth/dev/_demote")
                            .with(authentication(auth(user)))
                            .param("username", "maria")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("demoted")));

            var reloaded = peopleRepository.findByUserName("maria").orElseThrow();
            Assertions.assertEquals("ROLE_USER", reloaded.getRole());
        }

        @Test
        void demote_unknownUser_returns404() throws Exception {
            var admin = saveUser("admin", "admin@example.com", "ROLE_ADMIN");

            mvc.perform(patch("/auth/dev/_demote")
                            .with(authentication(auth(admin)))
                            .param("username", "ghost")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"))
                    .andExpect(jsonPath("$.path").value("/auth/dev/_demote"));
        }

        // ============ 401 UNAUTHORIZED
        /*@Test
        void demote_unauthorized_returns401() throws Exception {
            mvc.perform(patch("/auth/dev/_demote")
                            .param("username", "any")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.path").value("/auth/dev/_demote"));
        }*/

        @Test
        void demote_serviceThrows_returns500() throws Exception {
            var admin = saveUser("root", "root@example.com", "ROLE_ADMIN");
            var token = auth(admin);
            SecurityContextHolder.getContext().setAuthentication(token);
            try {
                doThrow(new RuntimeException("DB down")).when(peopleService).demoteToUserByUsername(anyString());

                mvc.perform(patch("/auth/dev/_demote")
                                .with(authentication(token))
                                .param("username", "admin")
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.status").value(500))
                        .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                        .andExpect(jsonPath("$.path").value("/auth/dev/_demote"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        /*@Test
        void demote_notAvailable_whenFeatureFlagDisabled() throws Exception {
            mvc.perform(patch("/auth/dev/_demote").param("username", "any").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }*/
    }
}

