package com.simple_online_store_backend.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.service.PersonDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JWTFilter extends OncePerRequestFilter {
    private final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    private final JWTUtil jwtUtil;
    private final PersonDetailsService personDetailsService;

    @Autowired
    public JWTFilter(JWTUtil jwtUtil, PersonDetailsService personDetailsService) {
        this.jwtUtil = jwtUtil;
        this.personDetailsService = personDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            String authHeader = request.getHeader("Authorization");
            System.out.println("Authorization header = " + authHeader);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                DecodedJWT decodedJWT = jwtUtil.validateToken(jwt);
                String username = decodedJWT.getClaim("username").asString();
                String role = decodedJWT.getClaim("role").asString();

                if (username != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = personDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // credentials (пароль) больше не нужен
                                    List.of(new SimpleGrantedAuthority(role))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception e) {
            logger.error("Error in JWT filter: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return; // обязательно выйти, иначе произойдёт ошибка "двойной отправки": будет отправлена и ошибка
            // и попытка filterChain.doFilter(request, response);
        }

        filterChain.doFilter(request, response); // должно быть ВНЕ try/catch
    }

}
