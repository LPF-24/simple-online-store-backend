package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.login.LoginRequestDTO;
import com.simple_online_store_backend.dto.person.JwtResponse;
import com.simple_online_store_backend.dto.person.PersonRequestDTO;
import com.simple_online_store_backend.dto.person.PersonResponseDTO;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.exception.ErrorUtil;
import com.simple_online_store_backend.exception.InvalidRefreshTokenException;
import com.simple_online_store_backend.security.JWTUtil;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import com.simple_online_store_backend.service.PersonDetailsService;
import com.simple_online_store_backend.service.RefreshTokenService;
import com.simple_online_store_backend.util.PersonValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.net.URI;
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
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Valid authentication",
                                            summary = "All required fields present",
                                            value = """
                                                  {
                                                    "username": "admin",
                                                    "password": "ChangeMe_123!"
                                                  }
                                                  """
                                    ),
                                    @ExampleObject(
                                            name = "Unauthorized - invalid credentials",
                                            summary = "Unauthorized - incorrect login or password",
                                            value = """
                                                  {
                                                    "username": "adm",
                                                    "password": "ChangeM"
                                                  }
                                                  """
                                    ),
                                    @ExampleObject(
                                            name = "Locked - the user has been blocked",
                                            summary = "User has been blocked (soft delete, deletion of a page at the user's request with the possibility of restoration)",
                                            value = """
                                                  {
                                                    "username": "blocked",
                                                    "password": "Test234!"
                                                  }
                                                  """
                                    )
                            }
                    )
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
    public ResponseEntity<?> authenticate(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
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
                .secure(true) // true - only via HTTPS
                .path("/") // Specifies that the cookie will be sent for all site paths (/api, /auth, etc.)
                .maxAge(Duration.ofDays(7))
                .sameSite("None")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            /*
            Note:
            Sets a cookie in the HTTP response (adds a Set-Cookie header) so that the browser will save it, since the client-side refresh token is stored in the browser's memory
            */

        // Return the access token and user ID in the response body
        return ResponseEntity.ok(new JwtResponse(accessToken, personDetails.getId(), personDetails.getUsername()));
    }

    @Operation(summary = "Register new user",
            description = "Creates a new user account",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Registration details",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PersonRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Valid registration",
                                            summary = "All required fields present",
                                            value = """
                                                  {
                                                    "userName": "maria12",
                                                    "password": "Test234!",
                                                    "phoneNumber": "+89100605867",
                                                    "email": "maria12@gmail.com",
                                                    "agreementAccepted": true,
                                                    "dateOfBirth": "2000-01-01"
                                                  }
                                                  """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid - missing agreement",
                                            summary = "agreementAccepted is required for registration",
                                            value = """
                                                  {
                                                    "userName": "maria12",
                                                    "password": "Test234!",
                                                    "email": "maria12@gmail.com"
                                                  }
                                                  """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "User data: authentication token, id, username",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "Created",
                                            summary = "User successfully logged in.",
                                            value = "{ \"id\": 3, \"userName\": \"maria12\",\n" +
                                                    "    \"dateOfBirth\": \"2000-01-01\",\n" +
                                                    "    \"phoneNumber\": \"+89100605867\",\n" +
                                                    "    \"email\": \"maria12@gmail.com\",\n" +
                                                    "    \"role\": \"ROLE_USER\"}"
                                    ))),
                    @ApiResponse(responseCode = "400", description = "Bad Request",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Bad Request",
                                            summary = "Example of 400 Bad Request",
                                            value = "{ \"status\": 400,\n" +
                                                    "    \"message\": \"agreementAccepted - You must accept the terms and conditions;\",\n" +
                                                    "    \"path\": \"/auth/registration\",\n" +
                                                    "    \"code\": \"VALIDATION_ERROR\"}"
                                    ))),
                    @ApiResponse(responseCode = "500", description = "Internal server error.",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponseDTO.class),
                                    examples = @ExampleObject(
                                            name = "Internal server error",
                                            summary = "Example of 500 Internal Server Error",
                                            value = "{ \"status\": 500,\n" +
                                                    "    \"message\": \"Internal server error\",\n" +
                                                    "    \"path\": \"/auth/registration\",\n" +
                                                    "    \"code\": \"INTERNAL_ERROR\" }"
                                    )))
            })
    @PostMapping("/registration")
    public ResponseEntity<?> performRegistration(@RequestBody @Valid PersonRequestDTO dto,
                                                          BindingResult bindingResult) {
        logger.info("Beginning of the method performRegistration");
        personValidator.validate(dto, bindingResult);
        logger.info("Validation with personValidator was successful");
        if (bindingResult.hasErrors())
            ErrorUtil.returnErrorsToClient(bindingResult);
        logger.info("Middle of the method");

        PersonResponseDTO saved = peopleService.register(dto);

        URI location = URI.create("/users/" + saved.getId());
        logger.info("End of the method performRegistration");
        return ResponseEntity.created(location).body(saved);
    }

    @Operation(
            summary = "Refresh user access token",
            description = """
                        Refreshes the user's access token using the refresh token stored in the browser cookie.
                        
                        ### How to test in Swagger UI
                        
                        **200 OK (success):**
                        1. Call `/auth/refresh-dev/_issue-refresh?username=<existing>` — browser receives a valid `refreshToken` cookie.
                        2. Call `/auth/refresh` — you'll get 200 and a new `"access_token"` in the response body.
                        
                        **400 MISSING_COOKIE (no cookie present):**
                        1. Call `/auth/refresh-dev/_clear-cookie` — removes the cookie.
                        2. Call `/auth/refresh` — you'll get 400 with `code: MISSING_COOKIE`.
                        
                        **401 INVALID_REFRESH_TOKEN (broken signature):**
                        1. Call `/auth/refresh-dev/_issue-invalid?username=<existing>` — sets a refresh cookie with invalid signature.
                        2. Call `/auth/refresh` — you'll get 401 with `code: INVALID_REFRESH_TOKEN`.
                        
                        **401 INVALID_REFRESH_TOKEN (cookie ≠ stored value):**
                        1. Call `/auth/refresh-dev/_desync-saved?username=<existing>` — creates a mismatch between cookie and storage.
                        2. Call `/auth/refresh` — you'll get 401 with `code: INVALID_REFRESH_TOKEN`.
                        
                        **401 TOKEN_EXPIRED (expired refresh token):**
                        1. Call `/auth/refresh-dev/_issue-expired?username=<existing>` — issues an expired refresh cookie.
                        2. Call `/auth/refresh` — you'll get 401 with `code: TOKEN_EXPIRED`.
                        
                        **401 USER_NOT_FOUND (username not found in DB):**
                        1. Call `/auth/refresh-dev/_issue-for-unknown?username=<non_existing>` — creates refresh for non-existent user.
                        2. Call `/auth/refresh` — you'll get 401 with `code: USER_NOT_FOUND`.
                        
                        **Notes:**
                        - Dev helper endpoints are only available when `demo.helpers.enabled=true`.
                        - Cookies are `HttpOnly`, `Secure`, and `SameSite=None`; Swagger UI must run with `credentials: include`.
                        - To re-run tests, just repeat the corresponding dev helper → `/auth/refresh` sequence.
                        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token successfully issued",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = "{ \"access_token\": \"...\" }")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Missing cookie",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(value = "{ \"status\":400, \"code\":\"MISSING_COOKIE\", \"message\":\"Required cookie 'refreshToken' is missing\", \"path\":\"/auth/refresh\" }")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Invalid/expired refresh token or user not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name="INVALID_REFRESH_TOKEN", value = "{ \"status\":401, \"code\":\"INVALID_REFRESH_TOKEN\", \"message\":\"Invalid refresh token\", \"path\":\"/auth/refresh\" }"),
                                    @ExampleObject(name="TOKEN_EXPIRED", value = "{ \"status\":401, \"code\":\"TOKEN_EXPIRED\", \"message\":\"The refresh token has expired.\", \"path\":\"/auth/refresh\" }"),
                                    @ExampleObject(name="USER_NOT_FOUND", value = "{ \"status\":401, \"code\":\"USER_NOT_FOUND\", \"message\":\"The username was not found.\", \"path\":\"/auth/refresh\" }")
                            }
                    )
            )
    })
    @Parameter(
            name = "refreshToken",
            in = ParameterIn.COOKIE,
            description = "Refresh token stored in the browser cookie",
            required = false,
            schema = @Schema(type = "string")
    )
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @Parameter(hidden = true)
            @CookieValue("refreshToken") String refreshToken) {
        String username = jwtUtil.validateRefreshToken(refreshToken).getClaim("username").asString();
        String role = personDetailsService.loadUserByUsername(username).getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        String savedToken = refreshTokenService.getRefreshToken(username);

        if (!refreshToken.equals(savedToken)) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        String newAccessToken = jwtUtil.generateToken(username, role);

        return ResponseEntity.ok(Map.of("access_token", newAccessToken));
    }

    @Operation(
            summary = "Logout (invalidate refresh token)",
            description = """
    Deletes user's refresh token in storage and sets the browser cookie for deletion.

    ### How to test in Swagger UI
    **200 OK (success):**
    1) Call `/auth/login` with valid credentials — browser receives `refreshToken` cookie.
    2) Call `/auth/logout` — you'll get 200 and `Set-Cookie` to clear the cookie.

    **400 MISSING_COOKIE (no cookie present):**
    - Open Swagger UI in Incognito **or**
    - DevTools → Application → Cookies → delete `refreshToken`, then call `/auth/logout`.

    **401 INVALID_REFRESH_TOKEN (broken token):**
    - After login, DevTools → Application → Cookies → edit `refreshToken` value to `abc.def.ghi`, then call `/auth/logout`.

    **Dev shortcuts (enabled on this environment):**
    - `POST /auth/logout-dev/_issue-refresh` → issues a valid refresh cookie for a demo user.
    - `POST /auth/logout-dev/_issue-invalid` → sets an invalid refresh cookie.
    - `POST /auth/logout-dev/_clear-cookie` → removes the cookie.
    Then call `/auth/logout` to see 200/401/400 respectively.
    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = "refreshToken cleared (Max-Age=0; HttpOnly; Secure; Path=/)"),
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{ \"message\": \"Logged out successfully\" }")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Missing cookie",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(value = "{ \"status\":400, \"code\":\"MISSING_COOKIE\", \"message\":\"Required cookie 'refreshToken' is missing\", \"path\":\"/auth/logout\" }")
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Invalid/expired refresh token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(value = "{ \"status\":401, \"code\":\"INVALID_REFRESH_TOKEN\", \"message\":\"Invalid refresh token\", \"path\":\"/auth/logout\" }")
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(value = "{ \"status\":500, \"code\":\"INTERNAL_ERROR\", \"message\":\"Internal server error\", \"path\":\"/auth/logout\" }")
                    )
            )
    })
    @Parameter(
            name = "refreshToken",
            in = ParameterIn.COOKIE,
            description = "Refresh token cookie set during login",
            required = false,
            schema = @Schema(type = "string")
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @Parameter(hidden = true)
            @CookieValue("refreshToken") String refreshToken) {
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
