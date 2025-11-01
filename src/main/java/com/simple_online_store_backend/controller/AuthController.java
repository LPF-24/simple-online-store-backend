package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.login.LoginRequestDTO;
import com.simple_online_store_backend.dto.person.JwtResponse;
import com.simple_online_store_backend.dto.person.LoginRequest;
import com.simple_online_store_backend.dto.person.PersonRequestDTO;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import com.simple_online_store_backend.service.PersonDetailsService;
import com.simple_online_store_backend.service.RefreshTokenService;
import com.simple_online_store_backend.util.PersonValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@Tag(name = "Authentication", description = "Endpoints for user authentication and authorization")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PeopleService peopleService;
    private final PersonValidator personValidator;
    private final RefreshTokenService refreshTokenService;
    private final PersonDetailsService personDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(JWTUtil jwtUtil, AuthenticationManager authenticationManager, PeopleService peopleService, PersonValidator personValidator, RefreshTokenService refreshTokenService, PersonDetailsService personDetailsService) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.peopleService = peopleService;
        this.personValidator = personValidator;
        this.refreshTokenService = refreshTokenService;
        this.personDetailsService = personDetailsService;
    }

    @Operation(summary = "Login a user",
            description = "Returns access token and sets refresh token cookie",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login credentials",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDTO.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User data: authentication token, id, username",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "OK",
                                            summary = "User successfully logged in.",
                                            value = "{ \"accessToken\": \"token...\", \"id\": \"1\", \"username\": \"admin\"}"
                                    ))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Unauthorized",
                                            summary = "Example of 401 Unauthorized",
                                            value = "{ \"status\": 401, \"message\": \"Invalid username or password\", \"path\": \"/auth/login\", \"code\": \"BAD_CREDENTIALS\"}"
                                    ))),
                    @ApiResponse(responseCode = "423", description = "Account blocking exception",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Locked error",
                                            summary = "Example of 423 Locked Error",
                                            value = "{ \"status\": 423, \"message\": \"Your account is deactivated. Would you like to restore it?\", \"path\": \"/auth/login\", \"code\": \"ACCOUNT_LOCKED\"}"
                                    ))),
                    @ApiResponse(responseCode = "500", description = "Internal server error.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Internal server error",
                                            summary = "Example of 500 Internal Server Error",
                                            value = "{ \"status\": 500, \"error\": \"Internal Server Error\", \"path\": \"/auth/login\" }"
                                    )))
            })
    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();
        String role = personDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        // Generates JWT access and refresh tokens for the authenticated user.
        String accessToken = jwtUtil.generateToken(personDetails.getUsername(), role);
        String refreshToken = jwtUtil.generateRefreshToken(personDetails.getUsername());

        //Save refresh token in Redis (by username)
        refreshTokenService.saveRefreshToken(personDetails.getUsername(), refreshToken);

        // Stores the refresh token in an HttpOnly cookie to prevent client-side access.
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true) // JS won't be able to read this cookie → XSS attack protection.
                .secure(false) // true - only via HTTPS
                .path("/") // Specifies that the cookie will be sent for all site paths (/api, /auth, etc.)
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict") // Cookies are not sent from external sites (hard protection)
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            /*
            Note:
            Sets a cookie in the HTTP response (adds a Set-Cookie header) so that the browser will save it, since the client-side refresh token is stored in the browser's memory
            */

        // Return the access token and user ID in the response body
        return ResponseEntity.ok(new JwtResponse(accessToken, personDetails.getId(), personDetails.getUsername()));
    }

    @Operation(summary = "Register new user", description = "Creates a new user account")
    @ApiResponse(responseCode = "200", description = "Successfully registered")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @PostMapping("/registration")
    public ResponseEntity<HttpStatus> performRegistration(@RequestBody @Valid PersonRequestDTO dto,
                                                          BindingResult bindingResult) {
        logger.info("Beginning of the method performRegistration");
        personValidator.validate(dto, bindingResult);
        logger.info("Validation with personValidator was successful");
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);
        logger.info("Middle of the method");

        peopleService.register(dto);
        logger.info("End of the method performRegistration");
        return ResponseEntity.ok(HttpStatus.OK);
    }

    @Operation(summary = "Refresh access token", description = "Generates a new access token using a valid refresh token.")
    @ApiResponse(responseCode = "200", description = "New access token successfully issued")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue("refreshToken") String refreshToken) {
        try {
            String username = jwtUtil.validateRefreshToken(refreshToken).getClaim("username").asString();
            String role = personDetailsService.loadUserByUsername(username).getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

            String savedToken = refreshTokenService.getRefreshToken(username);

            if (!refreshToken.equals(savedToken)) {
                throw new RuntimeException("RefreshToken is invalid or expired");
            }

            String newAccessToken = jwtUtil.generateToken(username, role);

            return ResponseEntity.ok(Map.of("access_token", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }
    }

    @Operation(summary = "Logout the user", description = "Invalidates the refresh token and deletes the cookie.")
    @ApiResponse(responseCode = "200", description = "Delete refresh token cookie")
    @ApiResponse(responseCode = "401", description = "User is not authorized")
    @ApiResponse(responseCode = "500", description = "Error inside method")
    @ApiResponse(responseCode = "423", description = "Account is locked (deactivated)")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue("refreshToken") String refreshToken) {
        String username = jwtUtil.validateRefreshToken(refreshToken).getClaim("username").asString();
        refreshTokenService.deleteRefreshToken(username);

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }
}
