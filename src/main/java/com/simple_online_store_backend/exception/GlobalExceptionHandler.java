package com.simple_online_store_backend.exception;

import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(ValidationException ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req.getRequestURI());
    }

    // 400 — ошибки парсинга тела/enum и т.п.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String msg = Optional.of(ex.getMostSpecificCause())
                .map(Throwable::getMessage).orElse("Malformed request");
        // часто в сообщении лишний stacktrace — можно обрезать по первой строке
        msg = msg.split("\n")[0];
        return error(HttpStatus.BAD_REQUEST, "MESSAGE_NOT_READABLE", msg, req.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingCookie(
            MissingRequestCookieException ex, HttpServletRequest req) {
        String msg = "Required cookie '" + ex.getCookieName() + "' is missing";
        return error(HttpStatus.BAD_REQUEST, "MISSING_COOKIE", msg, req.getRequestURI());
    }

    // 401 — неверные креды при логине
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid username or password", req.getRequestURI());
    }

    // 403 — нет прав
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage(), req.getRequestURI());
    }

    // 423 — аккаунт заблокирован/деактивирован
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponseDTO> handleLocked(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.LOCKED, "ACCOUNT_LOCKED",
                "Your account is deactivated. Would you like to restore it?",
                req.getRequestURI());
    }

    // 500 — дефолт
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAny(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", req.getRequestURI());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest req) {
        return error(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "The username was not found.", req.getRequestURI());
    }

    @ExceptionHandler(JWTDecodeException.class)
    public ResponseEntity<ErrorResponseDTO> handleJwtDecode(
            JWTDecodeException ex,
            jakarta.servlet.http.HttpServletRequest req) {
        return error(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                "Invalid refresh token",
                req.getRequestURI());
    }

    @ExceptionHandler(SignatureVerificationException.class)
    public ResponseEntity<ErrorResponseDTO> handleSignature(
            SignatureVerificationException ex,
            jakarta.servlet.http.HttpServletRequest req) {
        return error(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                "Invalid refresh token",
                req.getRequestURI());
    }

    @ExceptionHandler(AlgorithmMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleAlgMismatch(
            AlgorithmMismatchException ex,
            jakarta.servlet.http.HttpServletRequest req) {
        return error(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                "Invalid refresh token",
                req.getRequestURI());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponseDTO> handleExpired(
            com.auth0.jwt.exceptions.TokenExpiredException ex,
            jakarta.servlet.http.HttpServletRequest req) {
        return error(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "TOKEN_EXPIRED",
                "The refresh token has expired.",
                req.getRequestURI());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleEntityNotFound(
            jakarta.persistence.EntityNotFoundException ex,
            HttpServletRequest req) {
        return error(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", ex.getMessage(), req.getRequestURI());
    }

    private ResponseEntity<ErrorResponseDTO> error(HttpStatus status, String code, String message, String path) {
        ErrorResponseDTO dto = new ErrorResponseDTO();
        dto.setStatus(status.value());
        dto.setMessage(message);
        dto.setPath(path);
        dto.setCode(code);
        return ResponseEntity.status(status).body(dto);
    }
}
