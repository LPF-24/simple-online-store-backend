package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.person.LoginRequest;
import com.simple_online_store_backend.dto.person.PersonResponseDTO;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/people")
@RequiredArgsConstructor
public class PeopleController {
    private final PeopleService peopleService;
    private static final Logger logger = LoggerFactory.getLogger(PeopleController.class);

    @GetMapping("/all-customers")
    public List<PersonResponseDTO> getAllCustomers() {
        return peopleService.getAllConsumers();
    }

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

    @RequestMapping(value = "/restore-account", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> restoreAccount(@RequestBody LoginRequest loginRequest) {
        peopleService.restoreAccount(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok("Account successfully restored");
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/profile")
    public ResponseEntity<PersonResponseDTO> getProfile() {
        PersonResponseDTO response = peopleService.getCurrentUserInfo();
        return ResponseEntity.ok(response);
    }
}
