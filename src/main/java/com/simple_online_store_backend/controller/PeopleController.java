package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.login.LoginRequestDTO;
import com.simple_online_store_backend.dto.person.PersonResponseDTO;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "People", description = "Account management and user profile operations")
@RestController
@RequestMapping("/people")
public class PeopleController {
    private final PeopleService peopleService;
    private static final Logger logger = LoggerFactory.getLogger(PeopleController.class);

    public PeopleController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @Operation(
            summary = "Get all customers (admin)",
            description = """
        Returns a list of all registered users with the `ROLE_USER` role.

        Each item is a `PersonResponseDTO` (safe user profile) containing only:
        `id`, `userName`, `dateOfBirth`, `phoneNumber`, `email`, `role`.

        ### How to test in Swagger UI

        **200 OK (success):**
        1) `POST /auth/login` as a user with `ROLE_ADMIN` and copy the access token.
        2) Click **Authorize** → `Bearer <access_token>`.
        3) `GET /people/all-customers` → you'll get an array of customers (users with `ROLE_USER`).

        **401 UNAUTHORIZED:**
        - No `Authorization` header or malformed/expired access token → `401`.

        **403 FORBIDDEN:**
        - Authenticated but without `ROLE_ADMIN` (e.g. `ROLE_USER`) → `403`.

        **Notes:**
        - Endpoint is **admin-only** (`hasRole('ROLE_ADMIN')`).
        - The list contains **only** users with role `ROLE_USER`.
        - This endpoint uses the access token (not the refresh token).
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of customers returned successfully",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = PersonResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "OK",
                                    value = """
                                    [
                                      {
                                        "id": 12,
                                        "userName": "alice",
                                        "dateOfBirth": "1990-01-01",
                                        "phoneNumber": "+49-111",
                                        "email": "alice@example.com",
                                        "role": "ROLE_USER"
                                      },
                                      {
                                        "id": 27,
                                        "userName": "bob",
                                        "dateOfBirth": "1992-02-02",
                                        "phoneNumber": "+49-222",
                                        "email": "bob@example.com",
                                        "role": "ROLE_USER"
                                      }
                                    ]
                                    """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "MISSING_AUTH_HEADER", value = """
                                {
                                  "status": 401,
                                  "code": "MISSING_AUTH_HEADER",
                                  "message": "Missing or invalid Authorization header",
                                  "path": "/people/all-customers"
                                }"""),
                                    @ExampleObject(name = "INVALID_ACCESS_TOKEN", value = """
                                {
                                  "status": 401,
                                  "code": "INVALID_ACCESS_TOKEN",
                                  "message": "Invalid access token",
                                  "path": "/people/all-customers"
                                }"""),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = """
                                {
                                  "status": 401,
                                  "code": "TOKEN_EXPIRED",
                                  "message": "The access token has expired",
                                  "path": "/people/all-customers"
                                }""")
                            }
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "ACCESS_DENIED", value = """
                            {
                              "status": 403,
                              "code": "ACCESS_DENIED",
                              "message": "Access is denied",
                              "path": "/people/all-customers"
                            }""")
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "INTERNAL_ERROR", value = """
                            {
                              "status": 500,
                              "code": "INTERNAL_ERROR",
                              "message": "Internal server error",
                              "path": "/people/all-customers"
                            }""")
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/all-customers")
    public List<PersonResponseDTO> getAllCustomers() {
        return peopleService.getAllConsumers();
    }

    @Operation(
            summary = "Deactivate user account",
            description = "Deactivates the current user's account (soft delete)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @RequestMapping(value = "/deactivate-account", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<String> deactivateAccount() {
        logger.info("Method deactivateAccount started");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId = ((PersonDetails) authentication.getPrincipal()).getId();
        peopleService.deactivateUserAccount(userId);
        logger.info("Method deactivateAccount ended");

        return ResponseEntity.ok("Account has been deactivated.");
    }

    @Operation(
            summary = "Restore deactivated account",
            description = "Restores a deactivated account using username and password"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account restored successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or account is already active")
    })
    @RequestMapping(value = "/restore-account", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> restoreAccount(@RequestBody LoginRequestDTO loginRequest) {
        peopleService.restoreAccount(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok("Account successfully restored");
    }

    @Operation(
            summary = "Get current user profile",
            description = "Returns information about the authenticated user's account"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/profile")
    public ResponseEntity<PersonResponseDTO> getProfile() {
        PersonResponseDTO response = peopleService.getCurrentUserInfo();
        return ResponseEntity.ok(response);
    }
}

