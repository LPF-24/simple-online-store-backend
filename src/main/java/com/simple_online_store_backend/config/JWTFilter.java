package com.simple_online_store_backend.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PersonDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTUtil jwtUtil;
    private final PersonDetailsService personDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    private static final AntPathMatcher PATHS = new AntPathMatcher();

    private static final List<String> AUTH_WHITELIST = List.of(
            "/auth/login",
            "/auth/registration",
            "/auth/refresh",
            "/auth/logout",
            "/auth/dev/**",
            "/auth/logout-dev/**",
            "/auth/refresh-dev/**",
            "/people/restore-account",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/product"
    );

    @Autowired
    public JWTFilter(JWTUtil jwtUtil,
                     PersonDetailsService personDetailsService,
                     AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtUtil = jwtUtil;
        this.personDetailsService = personDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String context = request.getContextPath();
        String uri = request.getRequestURI();
        String path = (context != null && !context.isEmpty() && uri.startsWith(context))
                ? uri.substring(context.length())
                : uri;

        for (String pat : AUTH_WHITELIST) {
            if (PATHS.match(pat, path)) {
                // log.debug("JWTFilter skipped for whitelisted path {}", path);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header = {}", authHeader);

        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                chain.doFilter(request, response);
                return;
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                request.setAttribute("auth.error.code", "MISSING_AUTH_HEADER");
                request.setAttribute("auth.error.message", "Missing or invalid Authorization header");
                authenticationEntryPoint.commence(
                        request, response,
                        new InsufficientAuthenticationException("missing-authorization-header"));
                return;
            }

            String token = authHeader.substring(7);
            DecodedJWT decoded = jwtUtil.validateToken(token);
            String username = decoded.getClaim("username").asString();
            if (username == null || username.isBlank()) {
                request.setAttribute("auth.error.code", "INVALID_ACCESS_TOKEN");
                request.setAttribute("auth.error.message", "Invalid access token (missing username claim)");
                authenticationEntryPoint.commence(
                        request, response,
                        new InsufficientAuthenticationException("invalid-username-claim"));
                return;
            }

            PersonDetails user = (PersonDetails) personDetailsService.loadUserByUsername(username);

            if (!user.isAccountNonLocked()) {
                response.setStatus(423);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("""
                  {"status":423,"code":"ACCOUNT_LOCKED","message":"Your account is deactivated. Would you like to restore it?","path":"%s"}
                """.formatted(request.getRequestURI()));
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);

        } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
            log.warn("JWT expired: {}", e.getMessage());
            request.setAttribute("auth.error.code", "TOKEN_EXPIRED");
            request.setAttribute("auth.error.message", "The access token has expired");
            authenticationEntryPoint.commence(
                    request, response,
                    new InsufficientAuthenticationException("token-expired"));
        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            request.setAttribute("auth.error.code", "INVALID_ACCESS_TOKEN");
            request.setAttribute("auth.error.message", "Invalid access token");
            authenticationEntryPoint.commence(
                    request, response,
                    new InsufficientAuthenticationException("token-invalid"));
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            log.warn("User not found for token: {}", e.getMessage());
            request.setAttribute("auth.error.code", "USER_NOT_FOUND");
            request.setAttribute("auth.error.message", "The username was not found.");
            authenticationEntryPoint.commence(
                    request, response,
                    new InsufficientAuthenticationException("user-not-found"));
        } catch (Exception e) {
            log.error("Unexpected error in JWT filter", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
              {"status":500,"code":"INTERNAL_ERROR","message":"Internal server error","path":"%s"}
            """.formatted(request.getRequestURI()));
        }
    }
}