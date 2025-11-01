package com.simple_online_store_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple_online_store_backend.dto.person.LoginRequest;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.simple_online_store_backend.security.PersonDetails;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RefreshTokenService refreshTokenService; // будет заменён на FakeRefreshTokenService из TestConfig

    @MockitoBean(enforceOverride = true)
    AuthenticationManager authenticationManager;

    @MockitoBean com.simple_online_store_backend.security.JWTUtil jwtUtil; // фиксируем значения токенов

    @TestConfiguration
    static class TestConfig {
        @Bean
        RefreshTokenService refreshTokenService() {
            return new FakeRefreshTokenService();
        }
    }

    /** Простая in-memory замена Redis-хранилища рефреш-токенов для тестов */
    static class FakeRefreshTokenService extends RefreshTokenService {
        private final Map<String, String> store = new ConcurrentHashMap<>();
        private volatile boolean shouldThrow = false;

        public FakeRefreshTokenService() {
            super(null); // базовому сервису RedisTemplate не нужен, не используем
        }

        public void setShouldThrow(boolean val) { this.shouldThrow = val; }

        @Override
        public void saveRefreshToken(String username, String refreshToken) {
            if (shouldThrow) throw new RuntimeException("Simulated Redis failure");
            store.put(username, refreshToken);
        }

        @Override
        public String getRefreshToken(String username) {
            return store.get(username);
        }

        @Override
        public void deleteRefreshToken(String username) {
            store.remove(username);
        }
    }

    private static LoginRequest req(String u, String p) {
        var r = new LoginRequest();
        r.setUsername(u);
        r.setPassword(p);
        return r;
    }

    /** Создаём реальный PersonDetails (без моков), как это сделал бы UserDetailsService */
    private static PersonDetails principal(String username, int id, String role) {
        var p = new Person();
        p.setId(id);
        p.setUserName(username);    // важно: именно userName
        p.setPassword("pwd");
        p.setRole(role);
        p.setDeleted(false);
        return new PersonDetails(p);
    }

    @Nested
    class methodLoginTest {
        @BeforeEach
        void resetMocks() {
            Mockito.reset(authenticationManager, jwtUtil);
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("200 OK: returns the access token in the body and the HttpOnly refreshToken cookie; the refreshToken is saved.")
        void success() throws Exception {
            String username = "john";
            var id = 42;
            var role = "ROLE_USER";
            var access = "access-abc";
            var refresh = "refresh-xyz";

            var principal = principal(username, id, role);
            Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(auth);
            given(jwtUtil.generateToken(eq(username), eq(role))).willReturn(access);
            given(jwtUtil.generateRefreshToken(eq(username))).willReturn(refresh);

            mvc.perform(post("/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req(username, "pwd"))))
                    .andExpect(status().isOk())
                    // Тело: есть access-token и username
                    .andExpect(content().string(containsString(access)))
                    .andExpect(content().string(containsString(username)))
                    // Cookie: refreshToken с нужными атрибутами
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=" + refresh)))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=")));

            // refreshToken действительно сохранён (через заменённый FakeRefreshTokenService)
            var saved = ((FakeRefreshTokenService) refreshTokenService).getRefreshToken(username);
            assertEquals(refresh, saved, "refresh-token must be stored under the username key");

            // взаимодействия
            verify(jwtUtil).generateToken(username, role);
            verify(jwtUtil).generateRefreshToken(username);
        }

        @Test
        @DisplayName("401 Unauthorized: incorrect login/password")
        void badCredentials() throws Exception {
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willThrow(new BadCredentialsException("bad creds"));

            mvc.perform(post("/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req("john", "wrong"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(containsString("Invalid username or password")));

            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("423 Locked: the account has been deactivated")
        void locked() throws Exception {
            given(authenticationManager.authenticate(
                    argThat(t -> t instanceof UsernamePasswordAuthenticationToken
                            && Objects.equals(t.getPrincipal(), "john")
                            && Objects.equals(t.getCredentials(), "pwd"))
            )).willThrow(new LockedException("locked"));

            mvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req("john", "pwd"))))
                    .andExpect(status().isLocked())
                    .andExpect(content().string(containsString("Your account is deactivated")));

            verify(jwtUtil, never()).generateToken(anyString(), anyString());
            verify(jwtUtil, never()).generateRefreshToken(anyString());
        }

        @Test
        @DisplayName("500 Internal Server Error: failure to save refreshToken to storage")
        void internal_onRefreshSaveFailure() throws Exception {
            var username = "john";
            var principal = principal(username, 7, "ROLE_USER");
            var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(auth);
            given(jwtUtil.generateToken(eq(username), eq("ROLE_USER"))).willReturn("acc");
            given(jwtUtil.generateRefreshToken(eq(username))).willReturn("ref");

            ((FakeRefreshTokenService) refreshTokenService).setShouldThrow(true);

            mvc.perform(post("/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req(username, "pwd"))))
                    .andExpect(status().isInternalServerError());
        }
    }
}
