package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.login.LoginRequestDTO;
import com.simple_online_store_backend.dto.person.PersonResponseDTO;
import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
            description = """
        Deactivates the **currently authenticated user's** account (soft delete).

        This action marks the user as `deleted=true` and cancels all active orders
        (`PENDING`, `PROCESSING`) by setting their status to `CANCELLED`.

        The operation requires a valid **access token** in the `Authorization` header.

        ### How to test in Swagger UI

        **200 OK (success):**
        1. `POST /auth/login` as a regular user (`ROLE_USER`) and copy the access token.  
        2. Click **Authorize** → `Bearer <token>`.  
        3. `PATCH /people/deactivate-account` → returns `"Account has been deactivated."`.

        **401 UNAUTHORIZED:**
        - Missing `Authorization` header.  
        - Invalid or expired access token.  
        - User not logged in.

        **403 FORBIDDEN:**
        - Authenticated but without `ROLE_USER` (e.g. `ROLE_ADMIN`) → `403`.
        
        **Notes:**
        - The endpoint works only for authenticated users.
        - After deactivation, the user’s account becomes locked (`deleted=true`).
        - To restore the account, use `POST /people/restore-account`.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deactivated successfully",
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    name = "OK",
                                    value = "Account has been deactivated."
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
                                  "path": "/people/deactivate-account"
                                }"""),
                                    @ExampleObject(name = "INVALID_ACCESS_TOKEN", value = """
                                {
                                  "status": 401,
                                  "code": "INVALID_ACCESS_TOKEN",
                                  "message": "Invalid access token",
                                  "path": "/people/deactivate-account"
                                }"""),
                                    @ExampleObject(name = "TOKEN_EXPIRED", value = """
                                {
                                  "status": 401,
                                  "code": "TOKEN_EXPIRED",
                                  "message": "The access token has expired",
                                  "path": "/people/deactivate-account"
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
                              "path": "/people/deactivate-account"
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
                          "path": "/people/deactivate-account"
                        }""")
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/deactivate-account")
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
            description = """
        Restores a previously deactivated user account (soft delete).

        The endpoint is **public (permitAll)** and does not require an access token.
        Provide your **username** and **password** in the request body. On success, the
        account flag `deleted=false` is set and access is restored.

        ### How to test in Swagger UI

        **200 OK (success):**
        - `PATCH /people/restore-account` with body:
          ```json
          { "username": "user", "password": "user123!" }
          ```
          Response: `"Account successfully restored"`.

        **Error cases:**
        - `404 NOT FOUND` — user not found (`ENTITY_NOT_FOUND`). To get a **404**, enter the details of an existing user.
        - `409 CONFLICT` — account is already active (`ACCOUNT_ALREADY_ACTIVE`).Please make a request **twice** with **valid body for a regular user**.
        - `400 BAD REQUEST` — malformed JSON body (`MESSAGE_NOT_READABLE`). Select when making a JSON request that **will trigger 400 MESSAGE_NOT_READABLE**
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account successfully restored",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "OK",
                                    value = "Account successfully restored"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Malformed request body",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "MESSAGE_NOT_READABLE", value = """
                        {
                          "status": 400,
                          "code": "MESSAGE_NOT_READABLE",
                          "message": "JSON parse error: ...",
                          "path": "/people/restore-account"
                        }""")
                    )
            ),
            /*@ApiResponse(responseCode = "401", description = "Unauthorized / invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "BAD_CREDENTIALS", value = """
                        {
                          "status": 401,
                          "code": "BAD_CREDENTIALS",
                          "message": "Invalid username or password",
                          "path": "/people/restore-account"
                        }""")
                    )
            ),*/
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "ENTITY_NOT_FOUND", value = """
                        {
                          "status": 404,
                          "code": "ENTITY_NOT_FOUND",
                          "message": "User not found",
                          "path": "/people/restore-account"
                        }""")
                    )
            ),
            @ApiResponse(responseCode = "409", description = "Account already active",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "ACCOUNT_ALREADY_ACTIVE", value = """
                        {
                          "status": 409,
                          "code": "ACCOUNT_ALREADY_ACTIVE",
                          "message": "Account is already active",
                          "path": "/people/restore-account"
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
                          "path": "/people/restore-account"
                        }""")
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginRequestDTO.class),
                    examples = {
                            @ExampleObject(
                                    name = "ROLE_USER: All required fields present",
                                    summary = "Valid body for a regular user. When reused will trigger 404 NO_FOUND",
                                    value = """
                                      {
                                        "username": "user",
                                        "password": "user123!"
                                      }
                                      """
                            ),
                            /*@ExampleObject(
                                    name = "ROLE_ADMIN: All required fields present",
                                    summary = "Valid body for an admin user (also permitted)",
                                    value = """
                                      {
                                        "username": "admin",
                                        "password": "ChangeMe_123!"
                                      }
                                      """
                            ),*/
                            @ExampleObject(
                                    name = "Unauthorized - invalid credentials",
                                    summary = "Will trigger 404 NO_FOUND",
                                    value = """
                                      {
                                        "username": "usr",
                                        "password": "wrong"
                                      }
                                      """
                            ),
                            @ExampleObject(
                                    name = "Malformed JSON request body",
                                    summary = "Will trigger 400 MESSAGE_NOT_READABLE",
                                    value = """
                                      {
                                        username: user,
                                        "password": 12345
                                      }
                                      """
                            )
                    }
            )
    )
    @PatchMapping(
            value = "/restore-account",
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<?> restoreAccount(@RequestBody LoginRequestDTO loginRequest) {
        peopleService.restoreAccount(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok("Account successfully restored");
    }

    @Operation(
            summary = "Get current user's profile",
            description = """
            Returns the profile information of the currently authenticated user.

            ### How to test in Swagger UI

            **200 OK (success):**
            1. `POST /auth/login` as a user with `ROLE_USER` or `ROLE_ADMIN`.
            2. Copy the received access token → click **Authorize** → paste `Bearer <token>`.
            3. Call `GET /people/profile`.
            4. You’ll receive a JSON with the user’s profile (id, username, email, role, etc.).

            **401 UNAUTHORIZED:**
            - No access token provided (unauthenticated request).
            - Expired or malformed access token.

            **403 FORBIDDEN:**
            - Authenticated but does not have the proper authority (should not normally occur if configuration is `.authenticated()`).

            **404 NOT FOUND:**
            - The user from the JWT no longer exists in the database (was deleted manually or missing).

            **423 ACCOUNT_LOCKED:**
            - The account is deactivated (`deleted=true`).
            - To restore it, use `PATCH /people/restore-account`.

            **Notes:**
            - Requires a valid JWT token (bearerAuth).
            - Accessible to both `ROLE_USER` and `ROLE_ADMIN`.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user's profile",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PersonResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "OK",
                                    summary = "Successful profile retrieval",
                                    value = """
                                        {
                                          "id": 12,
                                          "userName": "alice",
                                          "email": "alice@test.io",
                                          "role": "ROLE_USER",
                                          "birthDate": "1990-01-01",
                                          "phoneNumber": "+49-111"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized (missing/invalid/expired token)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(name = "UNAUTHORIZED", summary = "No token provided", value = """
                                        {
                                          "status": 401,
                                          "code": "UNAUTHORIZED",
                                          "message": "Authentication is required to access this resource",
                                          "path": "/people/profile"
                                        }"""),
                                    @ExampleObject(name = "TOKEN_EXPIRED", summary = "Expired access token", value = """
                                        {
                                          "status": 401,
                                          "code": "TOKEN_EXPIRED",
                                          "message": "The access token has expired",
                                          "path": "/people/profile"
                                        }""")
                            })
            ),
            @ApiResponse(responseCode = "423", description = "Account locked/deactivated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "ACCOUNT_LOCKED", summary = "Soft-deleted account", value = """
                                {
                                  "status": 423,
                                  "code": "ACCOUNT_LOCKED",
                                  "message": "Your account is deactivated. Would you like to restore it?",
                                  "path": "/people/profile"
                                }""")
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(name = "INTERNAL_ERROR", summary = "Unhandled exception", value = """
                                {
                                  "status": 500,
                                  "code": "INTERNAL_ERROR",
                                  "message": "Internal server error",
                                  "path": "/people/profile"
                                }""")
                    )
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile")
    public ResponseEntity<PersonResponseDTO> getProfile() {
        PersonResponseDTO response = peopleService.getCurrentUserInfo();
        return ResponseEntity.ok(response);
    }
}

