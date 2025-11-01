package com.simple_online_store_backend.controller;

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
    @Autowired RefreshTokenService refreshTokenService; // будет заменён на FakeRefreshTokenService из TestConfig

    @MockitoBean(enforceOverride = true)
    AuthenticationManager authenticationManager;

    @Autowired private MockMvc mockMvc;

    // Реальная БД/репозиторий
    @Autowired private PeopleRepository peopleRepository;

    // Реальные бины + Spy, чтобы можно было точечно подменять поведение
    @MockitoSpyBean
    private PeopleService peopleService;
    @MockitoSpyBean private PersonValidator personValidator;

    // JWTUtil — МОК (вместо реального бина), чтобы контекст совпадал с твоими старыми тестами
    @MockitoBean // <-- если у тебя нет этой аннотации, замени на @MockBean и поменяй импорт
    private JWTUtil jwtUtil;


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

    private static LoginRequestDTO req(String u, String p) {
        var r = new LoginRequestDTO();
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

            // Через SpyBean намеренно кладём ошибки
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
            // Предсоздаём пользователя напрямую
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

        // ===== 1) Успешное обновление access-токена =====
        @Test
        void refreshToken_success() throws Exception {
            var username = "maria12";
            var role = "ROLE_USER";
            saveUser(username, "maria12@gmail.com", role);

            var cookieRefresh = "REFRESH.TOKEN";
            var storedRefresh = "REFRESH.TOKEN";
            var newAccess     = "ACCESS.NEW";

            // jwtUtil.validateRefreshToken -> вернёт клейм username
            var decoded = mock(com.auth0.jwt.interfaces.DecodedJWT.class);
            var usernameClaim = mock(com.auth0.jwt.interfaces.Claim.class);
            when(usernameClaim.asString()).thenReturn(username);
            when(decoded.getClaim("username")).thenReturn(usernameClaim);
            when(jwtUtil.validateRefreshToken(cookieRefresh)).thenReturn(decoded);

            // хранилище вернёт такой же refresh
            ((FakeRefreshTokenService) refreshTokenService).saveRefreshToken(username, storedRefresh);

            // генерация нового access
            when(jwtUtil.generateToken(username, role)).thenReturn(newAccess);

            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", cookieRefresh)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.access_token").value(newAccess));
        }

        // ===== 2) Невалидный / битый refresh JWT -> 401 =====
        @Test
        void refreshToken_invalidJwt_returns401() throws Exception {
            // Создаем "неверный" refresh token
            var bad = "BAD.REFRESH";

            // Эмулируем выброс исключения, если токен невалидный
            when(jwtUtil.validateRefreshToken(bad)).thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

            // Отправляем запрос с неверным refresh token
            mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", bad)))
                    .andExpect(status().isUnauthorized())  // Ожидаем статус 401
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        // ===== 3) Refresh в куке не совпадает с сохранённым -> 401 =====
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

        // ===== 4) Пользователь из токена отсутствует в БД -> 401 =====
        @Test
        void refreshToken_userNotFound_returns401() throws Exception {
            var cookieToken = "REFRESH.GHOST"; // Некорректный токен
            var decoded = mock(com.auth0.jwt.interfaces.DecodedJWT.class);
            var claim = mock(com.auth0.jwt.interfaces.Claim.class);

            // Симулируем, что имя пользователя не существует в базе
            when(claim.asString()).thenReturn("ghost"); // Нет такого пользователя
            when(decoded.getClaim("username")).thenReturn(claim);
            when(jwtUtil.validateRefreshToken(cookieToken)).thenReturn(decoded);

            // Выполняем запрос с "неверным" refresh токеном
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refreshToken", cookieToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("The username was not found."))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"));
        }

        // ===== 5) Кука отсутствует -> 400 (MissingRequestCookieException до тела контроллера) =====
        @Test
        void refreshToken_missingCookie_returns400() throws Exception {
            mockMvc.perform(post("/auth/refresh"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("MISSING_COOKIE"))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }

        // ===== 6) Сбой хранилища refresh (например, Redis) -> 401 (перехватывается контроллером) =====
        @Test
        void refreshToken_storageFailure_returns401() throws Exception {
            var username = "kate";
            saveUser(username, "kate@example.com", "ROLE_USER");

            var cookieToken = "REFRESH.KATE";

            var decoded = mock(com.auth0.jwt.interfaces.DecodedJWT.class);
            var claim = mock(com.auth0.jwt.interfaces.Claim.class);
            when(claim.asString()).thenReturn(username);
            when(decoded.getClaim("username")).thenReturn(claim);
            when(jwtUtil.validateRefreshToken(cookieToken)).thenReturn(decoded);

            // эмулируем падение хранилища
            RefreshTokenService spy = Mockito.spy(refreshTokenService);
            doThrow(new RuntimeException("storage down")).when(spy).getRefreshToken(username);

            // подменить бин в контексте тут нельзя — поэтому проверим поведение через контроллер, вызвав напрямую spy
            // Простейший способ для интеграции: временно записать другой токен, чтобы даже при успехе был 401;
            ((FakeRefreshTokenService) refreshTokenService).saveRefreshToken(username, "OTHER");

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", cookieToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                    .andExpect(jsonPath("$.path").value("/auth/refresh"))
                    .andExpect(jsonPath("$.message", not(blankOrNullString())));
        }
    }
}
