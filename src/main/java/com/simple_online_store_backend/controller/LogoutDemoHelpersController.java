package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Hidden;

import java.time.Duration;
import java.util.Map;

@ConditionalOnProperty(value = "demo.helpers.enabled", havingValue = "true")
@RestController
@RequestMapping("/auth")
// УБЕРИ @Hidden, если хочешь, чтобы HR видел их в Swagger.
// @Hidden
class LogoutDemoHelpersController {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    LogoutDemoHelpersController(JWTUtil jwtUtil, RefreshTokenService svc) {
        this.jwtUtil = jwtUtil; this.refreshTokenService = svc;
    }

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
        return ResponseEntity.ok(Map.of("message","Demo refresh issued"));
    }

    @PostMapping("/_issue-invalid")
    public ResponseEntity<?> issueInvalid(HttpServletResponse resp) {
        // header: {"alg":"HS256","typ":"JWT"}
        var header  = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        // payload: {"sub":"demo"}
        var payload = "eyJzdWIiOiJkZW1vIn0";
        // «подпись» заведомо неверна
        var invalidSig = "aW52YWxpZA"; // base64url("invalid")
        var bad = header + "." + payload + "." + invalidSig;

        var cookie = ResponseCookie.from("refreshToken", bad)
                .httpOnly(true).secure(true).sameSite("None")
                .path("/").maxAge(Duration.ofDays(7)).build();
        resp.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message","Invalid refresh set"));
    }

    @PostMapping("/_clear-cookie")
    public ResponseEntity<?> clear(HttpServletResponse resp) {
        var cookie = ResponseCookie.from("refreshToken","")
                .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
        resp.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message","Cookie cleared"));
    }
}

