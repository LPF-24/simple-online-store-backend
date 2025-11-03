package com.simple_online_store_backend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String code = (String) request.getAttribute("auth.error.code");
        String message = (String) request.getAttribute("auth.error.message");

        if (code == null) {
            code = "UNAUTHORIZED";
            message = "Authentication is required to access this resource";
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("status", 401);
        error.put("code", code);
        error.put("message", message);
        error.put("path", request.getRequestURI());

        response.getWriter().write(mapper.writeValueAsString(error));
    }
}
