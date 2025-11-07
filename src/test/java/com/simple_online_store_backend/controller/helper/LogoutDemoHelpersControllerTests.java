package com.simple_online_store_backend.controller.helper;

import com.simple_online_store_backend.service.RefreshTokenService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "demo.helpers.enabled=true",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
        "spring.data.redis.repositories.enabled=false"
})
class LogoutDemoHelpersControllerTests {

    @Autowired MockMvc mvc;

    @MockitoBean RefreshTokenService refreshTokenService;

    @BeforeEach
    void setup() {
        RedisConnection mockConn = mock(RedisConnection.class);

        RedisKeyCommands keyCmds = mock(RedisKeyCommands.class);
        when(mockConn.keyCommands()).thenReturn(keyCmds);
    }

    @Nested
    class IssueRefresh {

        @Test
        void issueRefresh_permitAll_returns200_andSetsValidCookie() throws Exception {
            mvc.perform(post("/auth/logout-dev/_issue-refresh")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Demo refresh issued"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=None"),
                            containsString("Path=/"),
                            containsString("Max-Age=")
                    )));
        }

        @Test
        void issueRefresh_serviceThrows_returns500() throws Exception {
            doThrow(new RuntimeException("Redis down"))
                    .when(refreshTokenService).saveRefreshToken(anyString(), anyString());

            mvc.perform(post("/auth/logout-dev/_issue-refresh")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                    .andExpect(jsonPath("$.path").value("/auth/logout-dev/_issue-refresh"));
        }
    }

    @Nested
    class IssueInvalid {

        @Test
        void issueInvalid_permitAll_returns200_andSetsBrokenCookie() throws Exception {
            mvc.perform(post("/auth/logout-dev/_issue-invalid")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Invalid refresh set"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=None"),
                            containsString("Path=/"),
                            containsString("Max-Age=")
                    )));
        }
    }

    @Nested
    class ClearCookie {

        @Test
        void clearCookie_permitAll_returns200_andRemovesCookie() throws Exception {
            mvc.perform(post("/auth/logout-dev/_clear-cookie")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Cookie cleared"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("Max-Age=0"),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=None"),
                            containsString("Path=/")
                    )));
        }
    }
}

