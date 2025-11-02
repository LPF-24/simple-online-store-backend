package com.simple_online_store_backend.controller.helper;

import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@ConditionalOnProperty(value = "demo.helpers.enabled", havingValue = "true")
@RestController
@RequestMapping("/auth/refresh-dev")
// @Hidden
public class RefreshDemoHelpersController {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public RefreshDemoHelpersController(JWTUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    // Настройки куки (удобно крутить в application.properties)
    @Value("${app.cookies.secure:true}")
    boolean cookieSecure;

    @Value("${app.cookies.same-site:None}")
    String cookieSameSite;

    // Секрет и issuer должны совпадать с тем, что использует JWTUtil
    @Value("${jwt_secret}")
    String jwtSecret;

    @Value("${jwt.issuer:ADMIN}")
    String jwtIssuer;

    private void setCookie(HttpServletResponse resp, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
        resp.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** 200 OK: выдать валидный refresh и сохранить его под username */
    @PostMapping("/_issue-refresh")
    @Operation(summary = "Issue a valid refresh cookie (200 path)")
    public ResponseEntity<?> issueValid(@RequestParam(defaultValue = "admin") String username,
                                        HttpServletResponse resp) {
        String refresh = jwtUtil.generateRefreshToken(username);
        refreshTokenService.saveRefreshToken(username, refresh);
        setCookie(resp, refresh, Duration.ofDays(7).toSeconds());
        return ResponseEntity.ok(Map.of("message", "Valid refresh issued for " + username));
    }

    /** 401 TOKEN_EXPIRED: refresh с истёкшим сроком (валидная подпись, exp в прошлом) */
    @PostMapping("/_issue-expired")
    @Operation(summary = "Issue an expired refresh cookie (401 TOKEN_EXPIRED)")
    public ResponseEntity<?> issueExpired(@RequestParam(defaultValue = "admin") String username,
                                          HttpServletResponse resp) {
        Algorithm alg = Algorithm.HMAC256(jwtSecret);
        Instant past = Instant.now().minusSeconds(600);
        String expired = JWT.create()
                .withSubject("RefreshToken")
                .withClaim("username", username)
                .withIssuedAt(new Date())
                .withIssuer(jwtIssuer)
                .withExpiresAt(Date.from(past))
                .sign(alg);
        refreshTokenService.saveRefreshToken(username, expired);
        setCookie(resp, expired, Duration.ofDays(7).toSeconds());
        return ResponseEntity.ok(Map.of("message", "Expired refresh issued for " + username));
    }

    @PostMapping("/_issue-invalid")
    @Operation(summary = "Issue an invalid-signature refresh cookie (401 INVALID_REFRESH_TOKEN)")
    public ResponseEntity<?> issueInvalidSignature(@RequestParam(defaultValue = "admin") String username,
                                                   HttpServletResponse resp) {
        Algorithm wrongAlg = Algorithm.HMAC256("WRONG_SECRET_FOR_DEMO_ONLY");
        String bad = JWT.create()
                .withSubject("RefreshToken")
                .withClaim("username", username)
                .withIssuedAt(new Date())
                .withIssuer(jwtIssuer)
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(wrongAlg);
        refreshTokenService.saveRefreshToken(username, bad);
        setCookie(resp, bad, Duration.ofDays(7).toSeconds());
        return ResponseEntity.ok(Map.of("message", "Invalid-signature refresh issued for " + username));
    }

    @PostMapping("/_desync-saved")
    @Operation(summary = "Desync saved refresh to force mismatch (401 INVALID_REFRESH_TOKEN)")
    public ResponseEntity<?> desyncSaved(@RequestParam(defaultValue = "admin") String username,
                                         HttpServletResponse resp) {
        // cookie — валидная
        String cookieRefresh = jwtUtil.generateRefreshToken(username);
        // в хранилище кладём другой валидный refresh
        String otherRefresh = jwtUtil.generateRefreshToken(username);
        refreshTokenService.saveRefreshToken(username, otherRefresh);

        setCookie(resp, cookieRefresh, Duration.ofDays(7).toSeconds());
        return ResponseEntity.ok(Map.of("message", "Cookie != saved; next /auth/refresh will be 401 INVALID_REFRESH_TOKEN"));
    }

    @PostMapping("/_clear-cookie")
    @Operation(summary = "Clear refresh cookie (400 MISSING_COOKIE)")
    public ResponseEntity<?> clearCookie(HttpServletResponse resp) {
        setCookie(resp, "", 0);
        return ResponseEntity.ok(Map.of("message", "Cookie cleared"));
    }

    @PostMapping("/_issue-for-unknown")
    @Operation(summary = "Issue valid refresh for non-existing user (401 USER_NOT_FOUND)")
    public ResponseEntity<?> issueForUnknown(@RequestParam(defaultValue = "ghost_user_xyz") String username,
                                             HttpServletResponse resp) {
        String refresh = jwtUtil.generateRefreshToken(username);
        refreshTokenService.saveRefreshToken(username, refresh);
        setCookie(resp, refresh, Duration.ofDays(7).toSeconds());
        return ResponseEntity.ok(Map.of("message", "Valid refresh issued for non-existing user " + username));
    }
}
