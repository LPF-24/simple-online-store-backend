package com.simple_online_store_backend.controller;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple_online_store_backend.dto.login.LoginRequestDTO;
import com.simple_online_store_backend.entity.Person;
import com.simple_online_store_backend.exception.GlobalExceptionHandler;
import com.simple_online_store_backend.exception.InvalidRefreshTokenException;
import com.simple_online_store_backend.repository.PeopleRepository;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.service.PeopleService;
import com.simple_online_store_backend.service.RefreshTokenService;
import com.simple_online_store_backend.util.PersonValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.simple_online_store_backend.dto.person.PersonRequestDTO;

import java.util.Optional;

import static org.hamcrest.Matchers.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class AuthControllerTests {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RefreshTokenService refreshTokenService;

    @MockitoBean(enforceOverride = true)
    AuthenticationManager authenticationManager;

    @Autowired private MockMvc mockMvc;

    @Autowired private PeopleRepository peopleRepository;

    @MockitoSpyBean
    private PeopleService peopleService;
    @MockitoSpyBean private PersonValidator personValidator;

    @MockitoBean
    private JWTUtil jwtUtil;


    @TestConfiguration
    static class TestConfig {
        @Bean
        RefreshTokenService refreshTokenService() {
            return new FakeRefreshTokenService();
        }
    }

    static class FakeRefreshTokenService extends RefreshTokenService {
        private final Map<String, String> store = new ConcurrentHashMap<>();
        private volatile boolean shouldThrow = false;

        public FakeRefreshTokenService() {
            super(null);
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

    private static LoginRequestDTO req(String u, String p) {
        var r = new LoginRequestDTO();
        r.setUsername(u);
        r.setPassword(p);
        return r;
    }

    private static PersonDetails principal(String username, int id, String role) {
        var p = new Person();
        p.setId(id);
        p.setUserName(username);
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
                    .andExpect(content().string(containsString(access)))
                    .andExpect(content().string(containsString(username)))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=" + refresh)))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=None")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=")));

            var saved = ((FakeRefreshTokenService) refreshTokenService).getRefreshToken(username);
            assertEquals(refresh, saved, "refresh-token must be stored under the username key");

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

    @Nested
    class methodPerformRegistrationTest {
        @AfterEach
        void cleanDbAndResetSpies() {
            peopleRepository.deleteAll();
            Mockito.reset(peopleService, personValidator);
        }

        @Test
        void registration_success() throws Exception {
            String body = """
                          {
                            "userName": "maria12",
                            "password": "Test234!",
                            "email": "maria12@gmail.com",
                            "agreementAccepted": true,
                            "dateOfBirth": "2000-01-01"
                          }
                        """;

            mockMvc.perform(post("/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

            Optional<Person> saved = peopleRepository.findByUserName("maria12");
            assertTrue(saved.isPresent(), "User must be persisted into DB after successful registration");
        }

        @Test
        void registration_validation_error_from_personValidator() throws Exception {
            String body = """
                          {
                            "userName": "m",
                            "password": "weak",
                            "email": "invalid"
                          }
                          """;

            doAnswer(inv -> {
                Errors errors = inv.getArgument(1, Errors.class);
                errors.rejectValue("userName", "Size", "username is too short");
                errors.rejectValue("email", "Email", "email format is invalid");
                return null;
            }).when(personValidator).validate(any(), any(Errors.class));

            mockMvc.perform(post("/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.path").value("/auth/registration"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        @Test
        void registration_duplicate_username() throws Exception {
            Person p = new Person();
            p.setUserName("maria12");
            p.setPassword("encoded");
            p.setEmail("maria12@gmail.com");
            p.setRole("ROLE_USER");

            peopleRepository.save(p);

            String body = """
                          {
                            "userName": "maria12",
                            "password": "Test234!",
                            "email": "maria12@gmail.com",
                            "agreementAccepted": true
                          }
                          """;

            mockMvc.perform(post("/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.path").value("/auth/registration"))
                    .andExpect(jsonPath("$.message", containsStringIgnoringCase("userName")));
        }

        @Test
        void registration_bad_json() throws Exception {
            String malformed = """
                              { "userName": "maria12", "password": "Test234!", "email": "maria12@gmail.com"
                            """;

            mockMvc.perform(post("/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformed))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("MESSAGE_NOT_READABLE"))
                    .andExpect(jsonPath("$.path").value("/auth/registration"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        @Test
        void registration_internal_error() throws Exception {
            String body = """
                          {
                            "userName": "john_d",
                            "password": "Strong123!",
                            "email": "john@example.com",
                            "agreementAccepted": true,
                            "dateOfBirth": "2000-01-01"
                          }
                        """;

            doThrow(new RuntimeException("DB is down"))
                    .when(peopleService).register(any(PersonRequestDTO.class));

            mockMvc.perform(post("/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("Internal server error"))
                    .andExpect(jsonPath("$.path").value("/auth/registration"));
        }

        @Test
        void registration_routes_and_serialization() throws Exception {
            String body = """
                          {
                            "userName": "route_check",
                            "password": "Strong123!",
                            "email": "route@check.dev",
                            "agreementAccepted": true,
                            "dateOfBirth": "2000-01-01"
                          }
                        """;

            mockMvc.perform(post("/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    class methodRefreshTokenTests {
        @AfterEach
        void cleanup() {
            peopleRepository.deleteAll();
            Mockito.reset(jwtUtil);
        }

        private void saveUser(String userName, String email, String role) {
            Person p = new Person();
            p.setUserName(userName);
            p.setEmail(email);
            p.setPassword("encoded");
            p.setRole(role);
            p.setDeleted(false);
            peopleRepository.save(p);
        }

        @Test
        void refreshToken_success() throws Exception {
            var username = "maria12";
            var role = "ROLE_USER";
            saveUser(username, "maria12@gmail.com", role);

            var cookieRefresh = "REFRESH.TOKEN";
            var storedRefresh = "REFRESH.TOKEN";
            var newAccess     = "ACCESS.NEW";

            var decoded = mock(com.auth0.jwt.interfaces.DecodedJWT.class);
            var usernameClaim = mock(com.auth0.jwt.interfaces.Claim.class);
            when(usernameClaim.asString()).thenReturn(username);
            when(decoded.getClaim("username")).thenReturn(usernameClaim);
            when(jwtUtil.validateRefreshToken(cookieRefresh)).thenReturn(decoded);

            ((FakeRefreshTokenService) refreshTokenService).saveRefreshToken(username, storedRefresh);

            when(jwtUtil.generateToken(username, role)).thenReturn(newAccess);

            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", cookieRefresh)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.access_token").value(newAccess));
        }

        @Test
        void refreshToken_invalidJwt_returns401() throws Exception {
            var bad = "BAD.REFRESH";

            when(jwtUtil.validateRefreshToken(bad)).thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", bad)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        @Test
        void refreshToken_mismatchStored_returns401() throws Exception {
            var username = "john";
            saveUser(username, "john@example.com", "ROLE_USER");

            var cookieToken = "COOKIE.REFRESH";
            var storedToken = "OTHER.REFRESH";

            var decoded = mock(com.auth0.jwt.interfaces.DecodedJWT.class);
            var claim = mock(com.auth0.jwt.interfaces.Claim.class);
            when(claim.asString()).thenReturn(username);
            when(decoded.getClaim("username")).thenReturn(claim);
            when(jwtUtil.validateRefreshToken(cookieToken)).thenReturn(decoded);

            ((FakeRefreshTokenService) refreshTokenService).saveRefreshToken(username, storedToken);

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", cookieToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                    .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"));
        }

        @Test
        void refreshToken_userNotFound_returns401() throws Exception {
            var cookieToken = "REFRESH.GHOST";
            var decoded = mock(com.auth0.jwt.interfaces.DecodedJWT.class);
            var claim = mock(com.auth0.jwt.interfaces.Claim.class);

            when(claim.asString()).thenReturn("ghost");
            when(decoded.getClaim("username")).thenReturn(claim);
            when(jwtUtil.validateRefreshToken(cookieToken)).thenReturn(decoded);

            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", cookieToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("The username was not found."))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"));
        }

        @Test
        void refreshToken_missingCookie_returns400() throws Exception {
            mockMvc.perform(post("/auth/refresh"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("MISSING_COOKIE"))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        @Test
        void refreshToken_expired_returns401() throws Exception {
            var cookie = "EXPIRED.REFRESH";
            when(jwtUtil.validateRefreshToken(cookie))
                    .thenThrow(new com.auth0.jwt.exceptions.TokenExpiredException("Expired", java.time.Instant.now()));

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", cookie)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        @Test
        void refreshToken_signatureInvalid_returns401() throws Exception {
            var bad = "HDR.PLD.SIG";
            when(jwtUtil.validateRefreshToken(bad))
                    .thenThrow(new com.auth0.jwt.exceptions.SignatureVerificationException(
                            com.auth0.jwt.algorithms.Algorithm.HMAC256("k")));

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", bad)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }

        @Test
        void refreshToken_notFoundInStorage_returns401() throws Exception {
            var username = "alice";
            saveUser(username, "alice@example.com", "ROLE_USER");
            var cookieToken = "COOKIE.REFRESH";

            var decoded = mock(DecodedJWT.class);
            var claim = mock(Claim.class);
            when(claim.asString()).thenReturn(username);
            when(decoded.getClaim("username")).thenReturn(claim);
            when(jwtUtil.validateRefreshToken(cookieToken)).thenReturn(decoded);

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", cookieToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        }
    }

    @Nested
    class methodLogoutTests {

        @AfterEach
        void cleanup() {
            Mockito.reset(jwtUtil);
        }

        @Test
        @DisplayName("logout: 200 OK, refresh token was removed from storage and the cookie was set for deletion")
        void logout_success_deletesRefresh_andClearsCookie() throws Exception {
            var username = "alice";
            var cookieRefresh = "REFRESH.ALICE";

            ((FakeRefreshTokenService) refreshTokenService).saveRefreshToken(username, cookieRefresh);

            var decoded = mock(DecodedJWT.class);
            var usernameClaim = mock(Claim.class);
            when(usernameClaim.asString()).thenReturn(username);
            when(decoded.getClaim("username")).thenReturn(usernameClaim);
            when(jwtUtil.validateRefreshToken(cookieRefresh)).thenReturn(decoded);

            mockMvc.perform(post("/auth/logout")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", cookieRefresh)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("Max-Age=0"),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("Path=/")
                    )));

            String storedAfter = ((FakeRefreshTokenService) refreshTokenService).getRefreshToken(username);
            assertEquals(null, storedAfter, "The refresh token must be deleted after logout");
        }

        @Test
        @DisplayName("logout: 400, if there is no cookie with refreshToken")
        void logout_missingCookie_returns400() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("MISSING_COOKIE"))
                    .andExpect(jsonPath("$.path").value("/auth/logout"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        @Test
        @DisplayName("logout: 401, if the refresh JWT is invalid")
        void logout_invalidRefresh_returns401() throws Exception {
            var bad = "BAD.REFRESH";
            when(jwtUtil.validateRefreshToken(bad))
                    .thenThrow(new com.simple_online_store_backend.exception.InvalidRefreshTokenException("Invalid refresh token"));

            mockMvc.perform(post("/auth/logout")
                            .cookie(new Cookie("refreshToken", bad)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                    .andExpect(jsonPath("$.path").value("/auth/logout"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        @Test
        @DisplayName("logout: 401, if the refresh JWT has expired")
        void logout_expiredRefresh_returns401() throws Exception {
            var expired = "EXPIRED.REFRESH";
            when(jwtUtil.validateRefreshToken(expired))
                    .thenThrow(new InvalidRefreshTokenException("The Token has expired"));

            mockMvc.perform(post("/auth/logout")
                            .cookie(new Cookie("refreshToken", expired)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                    .andExpect(jsonPath("$.path").value("/auth/logout"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }
    }
}
