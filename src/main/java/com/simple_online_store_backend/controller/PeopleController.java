package com.simple_online_store_backend.controller;

import com.simple_online_store_backend.dto.JwtResponse;
import com.simple_online_store_backend.dto.LoginRequest;
import com.simple_online_store_backend.dto.PersonResponseDTO;
import com.simple_online_store_backend.security.PersonDetails;
import com.simple_online_store_backend.service.PeopleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/all-customers")
    public List<PersonResponseDTO> getAllCustomers() {
        return peopleService.getAllConsumers();
    }

    @RequestMapping(value = "/deactivate-account", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<String> deactivateAccount() {
        System.out.println("Method deactivateAccount started");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int userId = ((PersonDetails) authentication.getPrincipal()).getId();
        peopleService.deactivateUserAccount(userId);
        System.out.println("Method deactivateAccount ended");

        return ResponseEntity.ok("Account has been deactivated.");
    }

    @RequestMapping(value = "/restore-account", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> restoreAccount(@RequestBody LoginRequest loginRequest) {
        peopleService.restoreAccount(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok("Account successfully restored");
    }
}
