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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JWTFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTUtil jwtUtil;
    private final PersonDetailsService personDetailsService;

    @Autowired
    public JWTFilter(JWTUtil jwtUtil, PersonDetailsService personDetailsService) {
        this.jwtUtil = jwtUtil;
        this.personDetailsService = personDetailsService;
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

                DecodedJWT decoded = jwtUtil.validateToken(token);
                String username = decoded.getClaim("username").asString();
                if (username == null || username.isBlank()) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token: missing username");
                    return;
                }

                PersonDetails user = (PersonDetails) personDetailsService.loadUserByUsername(username);

                if (!user.isAccountNonLocked()) {
                    throw new org.springframework.security.authentication.LockedException(
                            "Your account is deactivated. Would you like to restore it?");
                }
                /* Возможно, на будущее
                if (!user.isEnabled()) {
                    throw new org.springframework.security.authentication.DisabledException("Account is disabled");
                }*/

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(request, response);

        } catch (TokenExpiredException e) {
            log.warn("JWT expired: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "The token has expired");
        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        } catch (UsernameNotFoundException e) {
            log.warn("User not found for token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "The username was not found.");
        } catch (LockedException | DisabledException e) {
            log.warn("Account locked/disabled: {}", e.getMessage());
            response.sendError(423, "Your account is deactivated. Would you like to restore it?");
        } catch (Exception e) {
            log.error("Unexpected error in JWT filter", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}