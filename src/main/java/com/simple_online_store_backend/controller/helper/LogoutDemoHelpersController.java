package com.simple_online_store_backend.controller.helper;

import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.time.Duration;
import java.util.Map;

@ConditionalOnProperty(value = "demo.helpers.enabled", havingValue = "true")
@RestController
@RequestMapping("/auth/logout-dev")
class LogoutDemoHelpersController {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    LogoutDemoHelpersController(JWTUtil jwtUtil, RefreshTokenService svc) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = svc;
    }

    @Operation(
            summary = "Issue a valid refresh cookie (200 path)",
            description = """
            Issues a valid `refreshToken` cookie for testing `/auth/logout` with a successful 200 response.

            ### How to test
            1. Call this endpoint → browser receives valid `refreshToken` cookie.
            2. Then call `/auth/logout` → response 200 + cookie cleared.

            **Notes:**
            - Cookie: `HttpOnly`, `Secure`, `SameSite=None`
            - Default username: `demo`
            """
    )
    @PostMapping("/_issue-refresh")
    public ResponseEntity<?> issueRefresh(HttpServletResponse resp) {
        var username = "demo";
        var refresh = jwtUtil.generateRefreshToken(username);
        refreshTokenService.saveRefreshToken(username, refresh);
        var cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true).secure(true)
                .sameSite("None")
                .path("/").maxAge(Duration.ofDays(7)).build();
        resp.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Demo refresh issued"));
    }

    @Operation(
            summary = "Issue an invalid refresh cookie (401 path)",
            description = """
            Issues a refresh cookie with a broken signature, for testing `/auth/logout` → 401 INVALID_REFRESH_TOKEN.

            ### How to test
            1. Call this endpoint → browser receives invalid `refreshToken` cookie.
            2. Then call `/auth/logout` → response 401 INVALID_REFRESH_TOKEN.

            **Notes:**
            - Cookie: `HttpOnly`, `Secure`, `SameSite=None`
            """
    )
    @PostMapping("/_issue-invalid")
    public ResponseEntity<?> issueInvalid(HttpServletResponse resp) {
        var header  = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        var payload = "eyJzdWIiOiJkZW1vIn0";
        var invalidSig = "aW52YWxpZA"; // base64url("invalid")
        var bad = header + "." + payload + "." + invalidSig;

        var cookie = ResponseCookie.from("refreshToken", bad)
                .httpOnly(true).secure(true)
                .sameSite("None")
                .path("/").maxAge(Duration.ofDays(7)).build();
        resp.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Invalid refresh set"));
    }

    @Operation(
            summary = "Clear refresh cookie (400 path)",
            description = """
            Removes the `refreshToken` cookie to test `/auth/logout` → 400 MISSING_COOKIE.

            ### How to test
            1. Call this endpoint → cookie removed.
            2. Then call `/auth/logout` → response 400 MISSING_COOKIE.

            **Notes:**
            - Cookie cleared using `Max-Age=0`
            """
    )
    @PostMapping("/_clear-cookie")
    public ResponseEntity<?> clear(HttpServletResponse resp) {
        var cookie = ResponseCookie.from("refreshToken","")
                .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
        resp.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message","Cookie cleared"));
    }
}