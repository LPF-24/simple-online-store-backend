package com.simple_online_store_backend.controller.helper;

import com.simple_online_store_backend.service.RefreshTokenService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "demo.helpers.enabled=true",
        // JWTUtil secret used by generateRefreshToken
        "jwt_secret=test_test_test_secret",
        // predictable cookie attributes
        "app.cookies.secure=true",
        "app.cookies.same-site=None"
})
class RefreshDemoHelpersControllerTests {

    @Autowired MockMvc mvc;

    // Mock out refresh storage to avoid real Redis dependency
    @MockitoBean
    RefreshTokenService refreshTokenService;

    @BeforeEach
    void setup() {
        Mockito.reset(refreshTokenService);
        // default: successful save
        doNothing().when(refreshTokenService).saveRefreshToken(anyString(), anyString());
    }

    @Nested
    class IssueRefresh {
        @Test
        void issueRefresh_permitAll_returns200_andSetsCookie_andCallsService() throws Exception {
            mvc.perform(post("/auth/refresh-dev/_issue-refresh")
                            .param("username", "admin")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", containsString("Valid refresh issued")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=None"),
                            containsString("Path=/"),
                            containsString("Max-Age=")
                    )));

            verify(refreshTokenService).saveRefreshToken(anyString(), anyString());
        }

        @Test
        void issueRefresh_serviceThrows_returns500() throws Exception {
            doThrow(new RuntimeException("storage down"))
                    .when(refreshTokenService).saveRefreshToken(anyString(), anyString());

            mvc.perform(post("/auth/refresh-dev/_issue-refresh").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.code", anyOf(equalTo("INTERNAL_ERROR"), equalTo("SERVER_ERROR"))))
                    .andExpect(jsonPath("$.path").value("/auth/refresh-dev/_issue-refresh"));
        }
    }

    @Nested
    class IssueExpired {
        @Test
        void issueExpired_permitAll_returns200_andSetsExpiredCookie() throws Exception {
            mvc.perform(post("/auth/refresh-dev/_issue-expired")
                            .param("username", "admin")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", containsString("Expired refresh issued")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=None"),
                            containsString("Path=/"),
                            containsString("Max-Age=")
                    )));

            verify(refreshTokenService).saveRefreshToken(anyString(), anyString());
        }
    }

    @Nested
    class IssueInvalidSignature {
        @Test
        void issueInvalidSignature_permitAll_returns200_andSetsCookie() throws Exception {
            mvc.perform(post("/auth/refresh-dev/_issue-invalid")
                            .param("username", "admin")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", containsString("Invalid-signature refresh issued")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=None"),
                            containsString("Path=/"),
                            containsString("Max-Age=")
                    )));

            verify(refreshTokenService).saveRefreshToken(anyString(), anyString());
        }
    }

    @Nested
    class DesyncSaved {
        @Test
        void desyncSaved_permitAll_returns200_andSetsCookie() throws Exception {
            mvc.perform(post("/auth/refresh-dev/_desync-saved")
                            .param("username", "admin")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", containsString("Cookie != saved")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=None"),
                            containsString("Path=/"),
                            containsString("Max-Age=")
                    )));

            // saveRefreshToken called at least once for the saved 'other' token
            verify(refreshTokenService, Mockito.atLeastOnce()).saveRefreshToken(anyString(), anyString());
        }
    }

    @Nested
    class ClearCookie {
        @Test
        void clearCookie_permitAll_returns200_andRemovesCookie() throws Exception {
            mvc.perform(post("/auth/refresh-dev/_clear-cookie")
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

    @Nested
    class IssueForUnknown {
        @Test
        void issueForUnknown_permitAll_returns200_andSetsCookie() throws Exception {
            mvc.perform(post("/auth/refresh-dev/_issue-for-unknown")
                            // без параметра, берётся defaultValue из контроллера
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message", containsString("Valid refresh issued for non-existing user")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                            containsString("refreshToken="),
                            containsString("HttpOnly"),
                            containsString("Secure"),
                            containsString("SameSite=None"),
                            containsString("Path=/"),
                            containsString("Max-Age=")
                    )));

            verify(refreshTokenService).saveRefreshToken(anyString(), anyString());
        }
    }
}

