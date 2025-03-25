package com.simple_online_store_backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handlePersonException(ValidationException e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
