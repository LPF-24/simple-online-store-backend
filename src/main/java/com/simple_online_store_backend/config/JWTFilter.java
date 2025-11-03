package com.simple_online_store_backend.config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JWTFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTUtil jwtUtil;
    private final PersonDetailsService personDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint; // ⟵ добавили

    @Autowired
    public JWTFilter(JWTUtil jwtUtil,
                     PersonDetailsService personDetailsService,
                     AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtUtil = jwtUtil;
        this.personDetailsService = personDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");
        log.debug("Authorization header = {}", authHeader);

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String token = authHeader.substring(7);

                DecodedJWT decoded = jwtUtil.validateToken(token); // может кинуть TokenExpiredException / JWTVerificationException
                String username = decoded.getClaim("username").asString();
                if (username == null || username.isBlank()) {
                    request.setAttribute("auth.error.code", "INVALID_ACCESS_TOKEN");
                    request.setAttribute("auth.error.message", "Invalid access token (missing username claim)");
                    authenticationEntryPoint.commence(request, response,
                            new org.springframework.security.authentication.InsufficientAuthenticationException("invalid"));
                    return;
                }

                PersonDetails user = (PersonDetails) personDetailsService.loadUserByUsername(username);

                if (!user.isAccountNonLocked()) {
                    throw new org.springframework.security.authentication.LockedException(
                            "Your account is deactivated. Would you like to restore it?");
                }
                // если используешь enabled:
                // if (!user.isEnabled()) throw new DisabledException("Account is disabled");

                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(request, response);

        } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
            log.warn("JWT expired: {}", e.getMessage());
            request.setAttribute("auth.error.code", "TOKEN_EXPIRED");
            request.setAttribute("auth.error.message", "The access token has expired");
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException("expired"));
        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            request.setAttribute("auth.error.code", "INVALID_ACCESS_TOKEN");
            request.setAttribute("auth.error.message", "Invalid access token");
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException("invalid"));
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            log.warn("User not found for token: {}", e.getMessage());
            request.setAttribute("auth.error.code", "USER_NOT_FOUND");
            request.setAttribute("auth.error.message", "The username was not found.");
            authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.InsufficientAuthenticationException("user-not-found"));
        } catch (org.springframework.security.authentication.LockedException
                 | org.springframework.security.authentication.DisabledException e) {
            log.warn("Account locked/disabled: {}", e.getMessage());
            response.setStatus(423);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
              {"status":423,"code":"ACCOUNT_LOCKED","message":"Your account is deactivated. Would you like to restore it?","path":"%s"}
            """.formatted(request.getRequestURI()));
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