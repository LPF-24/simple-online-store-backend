package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.login.LoginRequestDTO;
import com.simple_online_store_backend.dto.person.PersonResponseDTO;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import io.swagger.v3.oas.annotations.Operation;
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
            summary = "Get all customers",
            description = "Returns a list of all registered users with the 'ROLE_USER' role"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of customers returned successfully")
    })
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

