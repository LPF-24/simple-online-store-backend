package com.simple_online_store_backend.controller.helper;

import com.simple_online_store_backend.exception.ErrorResponseDTO;
import com.simple_online_store_backend.service.PeopleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@ConditionalOnProperty(value = "demo.helpers.enabled", havingValue = "true")
@RestController
@RequestMapping("/auth/dev")
public class DevRoleController {
    private final PeopleService peopleService;

    public DevRoleController(PeopleService peopleService) { this.peopleService = peopleService; }

    @Operation(
            summary = "Demote user to ROLE_USER (demo-only)",
            description = """
        Sets the specified user's role to `ROLE_USER`.  
        Intended for demos/tests to quickly revert an admin back to a regular user.

        ### How to test
        1) Promote a user to admin (e.g., call `PATCH /people/promote` with a valid activation code).
        2) Call `PATCH /auth/dev/_demote?username=<username>` — you'll get a success message.
        3) Try any admin-only action → access is no longer allowed (role is now `ROLE_USER`).

        **Notes:**
        - Demo/testing only; **not for production**.
        - Available only when `demo.helpers.enabled=true`.
        - If the user is already `ROLE_USER`, the operation is effectively idempotent.
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Demoted successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "OK",
                                    value = """
                                        {
                                          "message": "User was demoted to ROLE_USER (dev)"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized (no/invalid/expired token)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "UNAUTHORIZED",
                                            summary = "No token provided",
                                            value = """
                                                {
                                                  "status": 401,
                                                  "code": "UNAUTHORIZED",
                                                  "message": "Authentication is required to access this resource",
                                                  "path": "/auth/dev/auth/dev"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "TOKEN_EXPIRED",
                                            summary = "Expired access token",
                                            value = """
                                                {
                                                  "status": 401,
                                                  "code": "TOKEN_EXPIRED",
                                                  "message": "The access token has expired",
                                                  "path": "/auth/dev/auth/dev"
                                                }
                                                """
                                    )
                            })),
            @ApiResponse(responseCode = "403", description = "Forbidden (user already has ROLE_USER or lacks access)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCESS_DENIED",
                                    summary = "Admin role already granted or access blocked",
                                    value = """
                                        {
                                          "status": 403,
                                          "code": "ACCESS_DENIED",
                                          "message": "Access is denied",
                                          "path": "/auth/dev/auth/dev"
                                        }
                                        """
                            ))),
            @ApiResponse(responseCode = "423", description = "Account is locked/deactivated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "ACCOUNT_LOCKED",
                                    summary = "LockedException from security filter",
                                    value = """
                                        {
                                          "status": 423,
                                          "code": "ACCOUNT_LOCKED",
                                          "message": "Your account is deactivated. Would you like to restore it?",
                                          "path": "/auth/dev/auth/dev"
                                        }
                                        """
                            ))),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "INTERNAL_ERROR",
                                    value = """
                                        {
                                          "status": 500,
                                          "code": "INTERNAL_ERROR",
                                          "message": "Internal server error",
                                          "path": "/auth/dev/_demote"
                                        }
                                        """
                            )
                    )
            )
    })
    @PatchMapping("/_demote")
    public ResponseEntity<?> demote(
            @Parameter(
                    name = "username",
                    description = "Username to demote (default: 'user')",
                    required = true,
                    example = "user" // ← предзаполнит поле в Swagger UI
            )
            @RequestParam String username
    ) {
        peopleService.demoteToUserByUsername(username);
        return ResponseEntity.ok(Map.of("message", "User was demoted to ROLE_USER (dev)"));
    }
}

